package com.couchbase.lite;

import com.couchbase.lite.internal.RevisionInternal;
import com.couchbase.lite.mockserver.MockDispatcher;
import com.couchbase.lite.mockserver.MockHelper;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.support.FileDirUtils;
import com.couchbase.lite.support.RevisionUtils;
import com.couchbase.lite.util.Log;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseTest extends LiteTestCaseWithDB {

    public void testPruneRevsToMaxDepthViaCompact() throws Exception {

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("testName", "testDatabaseCompaction");
        properties.put("tag", 1337);

        Document doc = createDocumentWithProperties(database, properties);
        SavedRevision rev = doc.getCurrentRevision();

        database.setMaxRevTreeDepth(1);
        for (int i = 0; i < 10; i++) {
            Map<String, Object> properties2 = new HashMap<String, Object>(properties);
            properties2.put("tag", i);
            rev = rev.createRevision(properties2);
        }

        database.compact();

        Document fetchedDoc = database.getDocument(doc.getId());
        List<SavedRevision> revisions = fetchedDoc.getRevisionHistory();
        assertEquals(1, revisions.size());
    }

    /**
     * When making inserts in a transaction, the change notifications should
     * be batched into a single change notification (rather than a change notification
     * for each insert)
     */
    public void testChangeListenerNotificationBatching() throws Exception {

        final int numDocs = 50;
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        database.addChangeListener(new Database.ChangeListener() {
            @Override
            public void changed(Database.ChangeEvent event) {
                atomicInteger.incrementAndGet();
            }
        });

        database.runInTransaction(new TransactionalTask() {
            @Override
            public boolean run() {
                createDocuments(database, numDocs);
                countDownLatch.countDown();
                return true;
            }
        });

        boolean success = countDownLatch.await(30, TimeUnit.SECONDS);
        assertTrue(success);

        assertEquals(1, atomicInteger.get());
    }

    /**
     * When making inserts outside of a transaction, there should be a change notification
     * for each insert (no batching)
     */
    public void testChangeListenerNotification() throws Exception {
        final int numDocs = 50;
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        database.addChangeListener(new Database.ChangeListener() {
            @Override
            public void changed(Database.ChangeEvent event) {
                atomicInteger.incrementAndGet();
            }
        });
        createDocuments(database, numDocs);
        assertEquals(numDocs, atomicInteger.get());
    }

    /**
     * Change listeners should only be called once no matter how many times they're added.
     */
    public void testAddChangeListenerIsIdempotent() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        Database.ChangeListener listener = new Database.ChangeListener() {
            @Override
            public void changed(Database.ChangeEvent event) {
                count.incrementAndGet();
            }
        };
        database.addChangeListener(listener);
        database.addChangeListener(listener);
        createDocuments(database, 1);
        assertEquals(1, count.intValue());
    }

    public void testGetActiveReplications() throws Exception {

        // create mock sync gateway that will serve as a pull target and return random docs
        int numMockDocsToServe = 0;
        MockDispatcher dispatcher = new MockDispatcher();
        MockWebServer server = MockHelper.getPreloadedPullTargetMockCouchDB(dispatcher, numMockDocsToServe, 1);
        dispatcher.setServerType(MockDispatcher.ServerType.COUCHDB);
        server.setDispatcher(dispatcher);
        try {
            server.play();

            final Replication replication = database.createPullReplication(server.getUrl("/db"));

            assertEquals(0, database.getAllReplications().size());
            assertEquals(0, database.getActiveReplications().size());

            final CountDownLatch replicationRunning = new CountDownLatch(1);
            replication.addChangeListener(new ReplicationActiveObserver(replicationRunning));

            replication.start();

            boolean success = replicationRunning.await(30, TimeUnit.SECONDS);
            assertTrue(success);

            assertEquals(1, database.getAllReplications().size());
            assertEquals(1, database.getActiveReplications().size());

            final CountDownLatch replicationDoneSignal = new CountDownLatch(1);
            replication.addChangeListener(new ReplicationFinishedObserver(replicationDoneSignal));

            success = replicationDoneSignal.await(60, TimeUnit.SECONDS);
            assertTrue(success);

            // workaround race condition.  Since our replication change listener will get triggered
            // _before_ the internal change listener that updates the activeReplications map, we
            // need to pause briefly to let the internal change listener to update activeReplications.
            Thread.sleep(500);

            assertEquals(1, database.getAllReplications().size());
            assertEquals(0, database.getActiveReplications().size());
        }finally {
            server.shutdown();
        }
    }

    public void testGetDatabaseNameFromPath() throws Exception {

        assertEquals("baz", FileDirUtils.getDatabaseNameFromPath("foo/bar/baz.cblite"));

    }

    public void testEncodeDocumentJSON() throws Exception {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("_local_seq", "");
        RevisionInternal revisionInternal = new RevisionInternal(props);
        byte[] encoded = RevisionUtils.asCanonicalJSON(revisionInternal);
        assertNotNull(encoded);
    }

    /**
     * in Database_Tests.m
     * - (void) test075_UpdateDocInTransaction
     */
    public void testUpdateDocInTransaction() throws InterruptedException {
        // Test for #256, "Conflict error when updating a document multiple times in transaction block"
        // https://github.com/couchbase/couchbase-lite-ios/issues/256

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("testName", "testUpdateDocInTransaction");
        properties.put("count", 1);

        final Document doc = createDocumentWithProperties(database, properties);

        final CountDownLatch latch = new CountDownLatch(1);
        database.addChangeListener(new Database.ChangeListener() {
            @Override
            public void changed(Database.ChangeEvent event) {
                Log.i(TAG, "-- changed() --");
                latch.countDown();
            }
        });
        assertTrue(database.runInTransaction(new TransactionalTask() {
            @Override
            public boolean run() {
                // Update doc. The currentRevision should update, but no notification be posted (yet).
                Map<String, Object> props1 = new HashMap<String, Object>();
                props1.putAll(doc.getProperties());
                props1.put("count", 2);
                SavedRevision rev1 = null;
                try {
                    rev1 = doc.putProperties(props1);
                } catch (CouchbaseLiteException e) {
                    Log.e(Log.TAG_DATABASE, e.toString());
                    return false;
                }
                assertNotNull(rev1);
                assertEquals(doc.getCurrentRevision(), rev1);
                assertEquals(1, latch.getCount());

                // Update doc again; this should succeed, in the same manner.
                Map<String, Object> props2 = new HashMap<String, Object>();
                props2.putAll(doc.getProperties());
                props2.put("count", 3);
                SavedRevision rev2 = null;
                try {
                    rev2 = doc.putProperties(props2);
                } catch (CouchbaseLiteException e) {
                    Log.e(Log.TAG_DATABASE, e.toString());
                    return false;
                }
                assertNotNull(rev2);
                assertEquals(doc.getCurrentRevision(), rev2);
                assertEquals(1, latch.getCount());

                return true;
            }
        }));
        assertTrue(latch.await(0, TimeUnit.SECONDS));
    }
}
