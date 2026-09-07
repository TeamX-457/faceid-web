package com.campus.security.faceid.model;

/**
 * Enumeration of user roles in the FaceID Campus Backend system.
 * 
 * UPLOADER: Mobile app users (security/duty staff) who capture and submit incident footage
 * ADMIN: Dashboard users (school authorities) who review incidents, confirm/reject matches, and manage students
 */
public enum Role {
    UPLOADER,  // Can only upload incidents
    ADMIN      // Full access: incidents, students, users
}
