package com.capstone.unitechhr.repositories

import com.capstone.unitechhr.models.ApplicationAnalysis
import com.capstone.unitechhr.models.ApplicationStatus
import com.capstone.unitechhr.models.Job
import com.capstone.unitechhr.models.SkillsMatch
import com.capstone.unitechhr.utils.asTask
import com.capstone.unitechhr.utils.voidTask
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argumentCaptor
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class ApplicationRepositoryTest {

    @Mock
    private lateinit var mockFirestore: FirebaseFirestore
    
    @Mock
    private lateinit var mockUserCollection: CollectionReference
    
    @Mock
    private lateinit var mockUserDocument: DocumentReference
    
    @Mock
    private lateinit var mockUserSnapshot: DocumentSnapshot
    
    @Mock 
    private lateinit var mockUniversitiesCollection: CollectionReference
    
    @Mock
    private lateinit var mockUniversityDocument: DocumentReference
    
    @Mock
    private lateinit var mockJobsCollection: CollectionReference
    
    @Mock
    private lateinit var mockJobDocument: DocumentReference
    
    @Mock
    private lateinit var mockApplicantsCollection: CollectionReference
    
    @Mock
    private lateinit var mockApplicantDocument: DocumentReference
    
    @Mock
    private lateinit var mockApplicationsCollection: CollectionReference
    
    @Mock
    private lateinit var mockApplicationsQuery: Query
    
    @Mock
    private lateinit var mockApplicationsQuerySnapshot: QuerySnapshot
    
    @Mock
    private lateinit var mockApplicationDocumentRef: DocumentReference
    
    @Mock
    private lateinit var mockJobRepository: JobRepository
    
    private lateinit var repository: ApplicationRepository
    
    private val testUserId = "test_user_id"
    private val testJobId = "test_job_id"
    private val testUniversityId = "test_university_id"
    
    @Before
    fun setup() {
        // Setup Firestore mocks
        `when`(mockFirestore.collection("users")).thenReturn(mockUserCollection)
        `when`(mockFirestore.collection("universities")).thenReturn(mockUniversitiesCollection)
        `when`(mockFirestore.collection("applications")).thenReturn(mockApplicationsCollection)
        
        // Setup user document mock
        `when`(mockUserCollection.document(testUserId)).thenReturn(mockUserDocument)
        `when`(mockUserDocument.get()).thenReturn(mockUserSnapshot.asTask())
        `when`(mockUserSnapshot.exists()).thenReturn(true)
        `when`(mockUserSnapshot.getString("firstName")).thenReturn("John")
        `when`(mockUserSnapshot.getString("lastName")).thenReturn("Doe")
        `when`(mockUserSnapshot.getString("email")).thenReturn("john.doe@example.com")
        
        // Setup universities collection mock
        `when`(mockUniversitiesCollection.document(testUniversityId)).thenReturn(mockUniversityDocument)
        `when`(mockUniversityDocument.collection("jobs")).thenReturn(mockJobsCollection)
        `when`(mockJobsCollection.document(testJobId)).thenReturn(mockJobDocument)
        `when`(mockJobDocument.collection("applicants")).thenReturn(mockApplicantsCollection)
        `when`(mockApplicantsCollection.document(testUserId)).thenReturn(mockApplicantDocument)
        `when`(mockApplicantDocument.set(any())).thenReturn(mockApplicantDocument.voidTask())
        
        // Setup applications query mock
        `when`(mockApplicationsCollection.whereEqualTo("userId", testUserId)).thenReturn(mockApplicationsQuery)
        `when`(mockApplicationsQuery.whereEqualTo("jobId", testJobId)).thenReturn(mockApplicationsQuery)
        `when`(mockApplicationsQuery.get()).thenReturn(mockApplicationsQuerySnapshot.asTask())
        
        // Setup application documents mock
        val mockApplicationDoc = mock(DocumentSnapshot::class.java)
        `when`(mockApplicationDoc.id).thenReturn("test_application_id")
        `when`(mockApplicationsQuerySnapshot.isEmpty).thenReturn(false)
        `when`(mockApplicationsQuerySnapshot.documents).thenReturn(listOf(mockApplicationDoc))
        `when`(mockApplicationsCollection.document("test_application_id")).thenReturn(mockApplicationDocumentRef)
        `when`(mockApplicationDocumentRef.update(anyString(), anyString())).thenReturn(mockApplicationDocumentRef.voidTask())
        
        // Setup JobRepository mock
        val testJob = Job(
            id = testJobId,
            title = "Test Job",
            universityId = testUniversityId,
            universityName = "Test University"
        )
        `when`(mockJobRepository.getJobs()).thenReturn(listOf(testJob))
        
        // Create repository instance
        repository = ApplicationRepository(mockFirestore)
        repository.setJobRepositoryForTesting(mockJobRepository)
    }
    
    @Test
    fun `updateApplicationStatusAndCopyToUniversity copies data to university job applicants collection`() = runTest {
        // Create test ApplicationAnalysis
        val testAnalysis = ApplicationAnalysis(
            id = "test_analysis_id",
            userId = testUserId,
            jobId = testJobId,
            jobTitle = "Test Job Title",
            resumeUrl = "https://example.com/resume.pdf",
            analysisDate = Date(),
            recommendation = "Recommend for interview",
            matchPercentage = "85"
        )
        
        // Call the method being tested through saveAnalysisToFirestore
        repository.testSaveAnalysisToFirestore(testAnalysis)
        
        // Verify user document was retrieved
        verify(mockUserDocument).get()
        
        // Verify applicant document was created with correct data
        val documentCaptor = argumentCaptor<Map<String, Any>>()
        verify(mockApplicantDocument).set(documentCaptor.capture())
        
        val capturedData = documentCaptor.firstValue
        assert(capturedData["name"] == "John Doe")
        assert(capturedData["email"] == "john.doe@example.com")
        assert(capturedData["resumeUrl"] == "https://example.com/resume.pdf")
        assert(capturedData["status"] == "Interview")
        assert(capturedData["matchPercentage"] == "85")
        assert(capturedData["userId"] == testUserId)
        
        // Verify application status was updated
        verify(mockApplicationsCollection).whereEqualTo("userId", testUserId)
        verify(mockApplicationsQuery).whereEqualTo("jobId", testJobId)
        verify(mockApplicationDocumentRef).update("status", "INTERVIEW")
    }
} 