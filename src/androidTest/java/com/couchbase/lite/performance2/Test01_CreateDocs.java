/**
 * Original iOS version by  Jens Alfke
 * Ported to Android by Marty Schoch
 *
 * Copyright (c) 2012 Couchbase, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.couchbase.lite.performance2;

import com.couchbase.lite.LitePerfTestCase;
import com.couchbase.lite.TransactionalTask;
import com.couchbase.lite.util.Log;


import java.util.HashMap;
import java.util.Map;

import com.couchbase.lite.Document;


public class Test01_CreateDocs extends LitePerfTestCase {

    public static final String TAG = "Test1_CreateDocs";

    private static final String _propertyValue = "1";

    public double runOne(final int numberOfDocuments, final int sizeOfDocuments) throws Exception {
        String[] bigObj = new String[sizeOfDocuments];
        for (int i = 0; i < sizeOfDocuments; i++) {
            bigObj[i] = _propertyValue;
        }
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("bigArray", bigObj);

        long startMillis = System.currentTimeMillis();
        boolean success = database.runInTransaction(new TransactionalTask() {
            public boolean run() {
                for (int i = 0; i < numberOfDocuments; i++) {
                    //create a document
                    try {
                        Document document = database.createDocument();
                        document.putProperties(props);
                    } catch (Throwable t) {
                        Log.v("PerformanceStats",TAG+", Document create failed", t);
                        return true;
                    }
                }

                return true;
            }
        });
        double executionTime = Long.valueOf(System.currentTimeMillis()-startMillis);
        //Log.v("PerformanceStats",TAG+","+executionTime+","+numberOfDocuments+","+sizeOfDocuments);
        return executionTime;
    }

}
