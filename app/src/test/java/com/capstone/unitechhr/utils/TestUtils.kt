package com.capstone.unitechhr.utils

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import org.mockito.Mockito

/**
 * Extension function to create a Task that returns this DocumentSnapshot
 */
fun DocumentSnapshot.asTask(): Task<DocumentSnapshot> {
    return Tasks.forResult(this)
}

/**
 * Extension function to create a Task that returns this QuerySnapshot
 */
fun QuerySnapshot.asTask(): Task<QuerySnapshot> {
    return Tasks.forResult(this)
}

/**
 * Extension function to create a void Task for DocumentReference
 */
fun DocumentReference.voidTask(): Task<Void> {
    return Tasks.forResult(null)
}

/**
 * Extension function to help with nested mocking
 */
inline fun <reified T> anyObject(): T {
    return Mockito.any(T::class.java)
} 