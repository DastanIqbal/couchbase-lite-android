/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_couchbase_lite_android_SQLiteJsonCollator */

#ifndef _Included_com_couchbase_lite_android_SQLiteJsonCollator
#define _Included_com_couchbase_lite_android_SQLiteJsonCollator
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_couchbase_lite_android_SQLiteJsonCollator
 * Method:    nativeRegister
 * Signature: (Ljava/lang/Object;Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_com_couchbase_lite_android_SQLiteJsonCollator_nativeRegister
  (JNIEnv *, jclass, jobject, jstring, jint);

/*
 * Class:     com_couchbase_lite_android_SQLiteJsonCollator
 * Method:    testCollateJSON
 * Signature: (IILjava/lang/String;ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_couchbase_lite_android_SQLiteJsonCollator_testCollateJSON
  (JNIEnv *, jclass, jint, jint, jstring, jint, jstring);

/*
 * Class:     com_couchbase_lite_android_SQLiteJsonCollator
 * Method:    testEscape
 * Signature: (Ljava/lang/String;)C
 */
JNIEXPORT jchar JNICALL Java_com_couchbase_lite_android_SQLiteJsonCollator_testEscape
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_couchbase_lite_android_SQLiteJsonCollator
 * Method:    testDigitToInt
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_couchbase_lite_android_SQLiteJsonCollator_testDigitToInt
  (JNIEnv *, jclass, jint);

#ifdef __cplusplus
}
#endif
#endif
