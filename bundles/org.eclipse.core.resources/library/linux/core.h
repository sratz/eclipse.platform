/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for core library */

#ifndef _Included_CORE_LIBRARY
#define _Included_CORE_LIBRARY
#ifdef __cplusplus
extern "C" {
#endif
#undef STAT_VALID
#define STAT_VALID 0x4000000000000000l
#undef STAT_FOLDER
#define STAT_FOLDER 0x2000000000000000l
#undef STAT_READ_ONLY
#define STAT_READ_ONLY 0x1000000000000000l
/*
 * Class:     org_eclipse_core_internal_localstore_CoreFileSystemLibrary
 * Method:    internalGetStat
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL Java_org_eclipse_core_internal_localstore_CoreFileSystemLibrary_internalGetStat
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     org_eclipse_core_internal_localstore_CoreFileSystemLibrary
 * Method:    internalSetReadOnly
 * Signature: ([BZ)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_core_internal_localstore_CoreFileSystemLibrary_internalSetReadOnly
   (JNIEnv *, jclass, jbyteArray, jboolean);

/*
 * Class:     org_eclipse_core_internal_localstore_CoreFileSystemLibrary
 * Method:    internalCopyAttributes
 * Signature: ([B[BZ)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_core_internal_localstore_CoreFileSystemLibrary_internalCopyAttributes
   (JNIEnv *, jclass, jbyteArray, jbyteArray, jboolean);

/*
 * Class:     org_eclipse_ant_core_EclipseProject
 * Method:    internalCopyAttributes
 * Signature: ([B[BZ)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_ant_core_EclipseProject_internalCopyAttributes
   (JNIEnv *, jclass, jbyteArray, jbyteArray, jboolean);

#ifdef __cplusplus
}
#endif
#endif
