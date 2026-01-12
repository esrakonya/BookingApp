package com.stellarforge.composebooking.utils

/**
 * Global constants for Firebase Collections and Configuration.
 *
 * THIS IS THE CONTROL CENTER FOR THE WHITE-LABEL APP.
 *
 * @property TARGET_BUSINESS_OWNER_ID **CRITICAL:** This ID links the Customer App to the specific Business Owner.
 * You must find the Owner's UID in the Firebase Console -> Authentication tab and paste it here.
 * If this ID does not match the Owner's UID, the calendar and services will appear empty.
 */
object FirebaseConstants {
    const val USERS_COLLECTION = "users"
    const val SERVICES_COLLECTION = "services"
    const val APPOINTMENTS_COLLECTION = "appointments"
    const val BOOKED_SLOTS_COLLECTION = "bookedSlots"
    const val BUSINESSES_COLLECTION = "businesses"

    // REPLACE THIS WITH YOUR REAL OWNER UID FROM FIREBASE CONSOLE
    const val TARGET_BUSINESS_OWNER_ID = "9HNXmWCUV7V9G5p0SBZ3lqTQ9Ne2"

}