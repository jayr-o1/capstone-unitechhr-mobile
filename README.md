# UniTechHR Mobile App

A mobile application for job seekers and university HR departments to streamline the application and recruitment process.

## Features

### Job Application Process

1. **Browsing Jobs**: Users can browse available jobs posted by universities.
2. **Application Submission**: Users can apply for jobs by submitting their resume.
3. **Application Analysis**: The system analyzes the resume against job requirements using AI.
4. **Application Status Tracking**: Users can track the status of their applications.
5. **Interview Scheduling**: The system supports scheduling interviews for qualified candidates.

### Applicant Management

The system automatically manages applicant profiles and status:

-   **Application Analysis**: When a user applies for a job, the system analyzes their resume and scores it against the job requirements.
-   **Status Updates**: Based on the analysis score, the application status may be updated.
-   **University Access**: When an applicant receives a status of "Interview" (either automatically through scoring or manually by an admin), their profile is automatically copied to the university's job applicants collection.

### University Applicant Collection

For each job in a university, qualified applicants are stored in a Firestore subcollection with the following structure:

```
universities/{universityId}/jobs/{jobId}/applicants/{userId}
```

Each applicant document contains:

```javascript
{
  name: "John Doe",
  email: "john.doe@example.com",
  dateApplied: "2023-10-01",
  resumeUrl: "https://example.com/resume/john-doe",
  status: "Interview",
  matchPercentage: "85",
  userId: "user123"
}
```

This allows university HR staff to easily access and review qualified candidates directly from their dashboard.

## Technical Implementation

-   **Platform**: Android (Kotlin)
-   **Database**: Firebase Firestore
-   **Authentication**: Firebase Authentication
-   **Storage**: Firebase Cloud Storage
-   **Resume Analysis**: Custom API with AI-based matching

## Getting Started

1. Clone the repository
2. Set up a Firebase project and add the google-services.json file to the app directory
3. Build and run the application in Android Studio
