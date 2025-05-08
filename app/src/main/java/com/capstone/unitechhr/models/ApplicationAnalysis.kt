package com.capstone.unitechhr.models

import com.google.firebase.firestore.PropertyName
import java.util.Date

data class ApplicationAnalysis(
    val id: String = "",
    val userId: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val resumeUrl: String = "",
    val analysisDate: Date = Date(),
    val displayName: String? = null,
    
    // Analysis data
    @get:PropertyName("match_percentage")
    @set:PropertyName("match_percentage")
    var matchPercentage: String = "",
    
    @get:PropertyName("recommendation")
    @set:PropertyName("recommendation")
    var recommendation: String = "",
    
    @get:PropertyName("skills_match")
    @set:PropertyName("skills_match")
    var skillsMatch: SkillsMatch = SkillsMatch(),
    
    @get:PropertyName("experience")
    @set:PropertyName("experience")
    var experience: Experience = Experience(),
    
    @get:PropertyName("education")
    @set:PropertyName("education")
    var education: Education = Education(),
    
    @get:PropertyName("certifications")
    @set:PropertyName("certifications")
    var certifications: Certifications? = Certifications(),
    
    @get:PropertyName("keywords")
    @set:PropertyName("keywords")
    var keywords: Keywords? = Keywords(),
    
    @get:PropertyName("industry")
    @set:PropertyName("industry")
    var industry: Industry? = Industry(),
    
    @get:PropertyName("benchmark")
    @set:PropertyName("benchmark")
    var benchmark: Benchmark? = Benchmark(),
    
    @get:PropertyName("improvement_suggestions")
    @set:PropertyName("improvement_suggestions")
    var improvementSuggestions: ImprovementSuggestions? = ImprovementSuggestions(),
    
    @get:PropertyName("confidence_scores")
    @set:PropertyName("confidence_scores")
    var confidenceScores: ConfidenceScores? = ConfidenceScores(),
    
    @get:PropertyName("salary_estimate")
    @set:PropertyName("salary_estimate")
    var salaryEstimate: SalaryEstimate? = SalaryEstimate(),
    
    @get:PropertyName("passing_details")
    @set:PropertyName("passing_details")
    var passingDetails: PassingDetails? = PassingDetails()
)

data class SkillsMatch(
    @get:PropertyName("matched_skills")
    @set:PropertyName("matched_skills")
    var matchedSkills: List<String> = listOf(),
    
    @get:PropertyName("missing_skills")
    @set:PropertyName("missing_skills")
    var missingSkills: List<String> = listOf(),
    
    @get:PropertyName("additional_skills")
    @set:PropertyName("additional_skills")
    var additionalSkills: List<String> = listOf(),
    
    @get:PropertyName("match_ratio")
    @set:PropertyName("match_ratio")
    var matchRatio: String = ""
)

data class Experience(
    @get:PropertyName("required_years")
    @set:PropertyName("required_years")
    var requiredYears: String = "",
    
    @get:PropertyName("applicant_years")
    @set:PropertyName("applicant_years")
    var applicantYears: String = "",
    
    @get:PropertyName("percentage_impact")
    @set:PropertyName("percentage_impact")
    var percentageImpact: String = "",
    
    @get:PropertyName("job_titles")
    @set:PropertyName("job_titles")
    var jobTitles: List<String> = listOf(),
    
    @get:PropertyName("durations")
    @set:PropertyName("durations")
    var durations: List<Duration> = listOf()
)

data class Duration(
    @get:PropertyName("start_date")
    @set:PropertyName("start_date")
    var startDate: String = "",
    
    @get:PropertyName("end_date")
    @set:PropertyName("end_date")
    var endDate: String = "",
    
    @get:PropertyName("text")
    @set:PropertyName("text")
    var text: String = "",
    
    @get:PropertyName("months")
    @set:PropertyName("months")
    var months: Int = 0
)

data class Education(
    @get:PropertyName("requirement")
    @set:PropertyName("requirement")
    var requirement: String = "",
    
    @get:PropertyName("applicant_education")
    @set:PropertyName("applicant_education")
    var applicantEducation: String = "",
    
    @get:PropertyName("assessment")
    @set:PropertyName("assessment")
    var assessment: String = ""
)

data class Certifications(
    @get:PropertyName("relevant_certs")
    @set:PropertyName("relevant_certs")
    var relevantCerts: List<String> = listOf(),
    
    @get:PropertyName("percentage_impact")
    @set:PropertyName("percentage_impact")
    var percentageImpact: String = ""
)

data class Keywords(
    @get:PropertyName("matched")
    @set:PropertyName("matched")
    var matched: String = "",
    
    @get:PropertyName("total")
    @set:PropertyName("total")
    var total: String = "",
    
    @get:PropertyName("match_ratio")
    @set:PropertyName("match_ratio")
    var matchRatio: String = ""
)

data class Industry(
    @get:PropertyName("detected")
    @set:PropertyName("detected")
    var detected: String = "",
    
    @get:PropertyName("confidence")
    @set:PropertyName("confidence")
    var confidence: String = ""
)

data class Benchmark(
    @get:PropertyName("industry")
    @set:PropertyName("industry")
    var industry: String = "",
    
    @get:PropertyName("benchmarks")
    @set:PropertyName("benchmarks")
    var benchmarks: BenchmarkScores = BenchmarkScores()
)

data class BenchmarkScores(
    @get:PropertyName("skills")
    @set:PropertyName("skills")
    var skills: Int = 0,
    
    @get:PropertyName("experience")
    @set:PropertyName("experience")
    var experience: Int = 0,
    
    @get:PropertyName("education")
    @set:PropertyName("education")
    var education: Int = 0,
    
    @get:PropertyName("overall")
    @set:PropertyName("overall")
    var overall: Int = 0
)

data class ImprovementSuggestions(
    @get:PropertyName("skills")
    @set:PropertyName("skills")
    var skills: List<String> = listOf(),
    
    @get:PropertyName("experience")
    @set:PropertyName("experience")
    var experience: List<String> = listOf(),
    
    @get:PropertyName("education")
    @set:PropertyName("education")
    var education: List<String> = listOf(),
    
    @get:PropertyName("general")
    @set:PropertyName("general")
    var general: List<String> = listOf()
)

data class ConfidenceScores(
    @get:PropertyName("skills_match")
    @set:PropertyName("skills_match")
    var skillsMatch: Double = 0.0,
    
    @get:PropertyName("experience_match")
    @set:PropertyName("experience_match")
    var experienceMatch: Double = 0.0,
    
    @get:PropertyName("education_match")
    @set:PropertyName("education_match")
    var educationMatch: Double = 0.0,
    
    @get:PropertyName("overall")
    @set:PropertyName("overall")
    var overall: Double = 0.0
)

data class SalaryEstimate(
    @get:PropertyName("min")
    @set:PropertyName("min")
    var min: Int = 0,
    
    @get:PropertyName("max")
    @set:PropertyName("max")
    var max: Int = 0,
    
    @get:PropertyName("currency")
    @set:PropertyName("currency")
    var currency: String = "",
    
    @get:PropertyName("note")
    @set:PropertyName("note")
    var note: String = ""
)

// New model for passing details - why applicant passed the scoring system
data class PassingDetails(
    @get:PropertyName("job_title")
    @set:PropertyName("job_title")
    var jobTitle: String = "",
    
    @get:PropertyName("match_percentage")
    @set:PropertyName("match_percentage")
    var matchPercentage: Int = 0,
    
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = "",
    
    @get:PropertyName("key_strengths")
    @set:PropertyName("key_strengths")
    var keyStrengths: List<String> = listOf(),
    
    @get:PropertyName("skills_analysis")
    @set:PropertyName("skills_analysis")
    var skillsAnalysis: SkillsAnalysis = SkillsAnalysis(),
    
    @get:PropertyName("experience_analysis")
    @set:PropertyName("experience_analysis")
    var experienceAnalysis: ExperienceAnalysis = ExperienceAnalysis(),
    
    @get:PropertyName("education_analysis")
    @set:PropertyName("education_analysis")
    var educationAnalysis: EducationAnalysis = EducationAnalysis()
)

data class SkillsAnalysis(
    @get:PropertyName("matched_count")
    @set:PropertyName("matched_count")
    var matchedCount: Int = 0,
    
    @get:PropertyName("missing_count")
    @set:PropertyName("missing_count")
    var missingCount: Int = 0,
    
    @get:PropertyName("match_impact")
    @set:PropertyName("match_impact")
    var matchImpact: String = ""
)

data class ExperienceAnalysis(
    @get:PropertyName("meets_requirement")
    @set:PropertyName("meets_requirement")
    var meetsRequirement: Boolean = false,
    
    @get:PropertyName("impact")
    @set:PropertyName("impact")
    var impact: String = ""
)

data class EducationAnalysis(
    @get:PropertyName("meets_requirement")
    @set:PropertyName("meets_requirement")
    var meetsRequirement: Boolean = false,
    
    @get:PropertyName("impact")
    @set:PropertyName("impact")
    var impact: String = ""
) 