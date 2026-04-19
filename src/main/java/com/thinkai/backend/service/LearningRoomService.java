package com.thinkai.backend.service;

import com.thinkai.backend.dto.CourseDetailResponse;
import com.thinkai.backend.dto.LessonTutorSummaryResponse;
import com.thinkai.backend.dto.LessonDetailResponse;
import com.thinkai.backend.dto.LessonResponse;
import com.thinkai.backend.dto.AISummarizeRequest;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.Enrollment;
import com.thinkai.backend.entity.Lesson;
import com.thinkai.backend.entity.LessonProgress;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.LessonProgressRepository;
import com.thinkai.backend.repository.LessonRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LearningRoomService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final AITutorService aiTutorService;
    private static final int SUMMARY_MAX_CHARS = 12000;
    private static final Pattern YOUTUBE_ID_PATTERN = Pattern.compile(
            "(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|embed/|shorts/))([A-Za-z0-9_-]{11})");
    private static final Pattern YOUTUBE_TEXT_PATTERN = Pattern.compile("<text[^>]*>(.*?)</text>");

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseWithLessons(String email, Long courseId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("Course not found", HttpStatus.NOT_FOUND));
        if (course.getStatus() == Course.Status.BLOCKED) {
            throw new ApiException("Khóa học đã bị khóa bởi quản trị viên", HttpStatus.FORBIDDEN);
        }

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new ApiException("You are not enrolled in this course", HttpStatus.FORBIDDEN));

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<LessonProgress> progresses = lessonProgressRepository.findByUserIdAndIsCompletedTrue(user.getId());

        List<LessonResponse> lessonResponses = new ArrayList<>();
        for (Lesson lesson : lessons) {
            boolean isCompleted = progresses.stream()
                    .anyMatch(p -> p.getLessonId().equals(lesson.getId()));

            lessonResponses.add(LessonResponse.builder()
                    .id(lesson.getId())
                    .title(lesson.getTitle())
                    .type(lesson.getType() != null ? lesson.getType().name() : null)
                    .duration(lesson.getDurationSeconds() != null ? lesson.getDurationSeconds() / 60 + " min" : null)
                    .isCompleted(isCompleted)
                    .orderIndex(lesson.getOrderIndex())
                    .build());
        }

        return CourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .price(course.getPrice())
                .isEnrolled(true)
                // Requires instructor mapping, skip if not mapped in entity directly
                // .instructorName(course.getInstructor() != null ? course.getInstructor().getFullName() : null)
                .progressPercent(enrollment.getProgressPercent())
                .lessons(lessonResponses)
                .build();
    }

    @Transactional
    public LessonDetailResponse getLessonDetail(String email, Long lessonId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException("Lesson not found", HttpStatus.NOT_FOUND));

        Course course = courseRepository.findById(lesson.getCourseId())
                .orElseThrow(() -> new ApiException("Course not found", HttpStatus.NOT_FOUND));
        if (course.getStatus() == Course.Status.BLOCKED) {
            throw new ApiException("Khóa học đã bị khóa bởi quản trị viên", HttpStatus.FORBIDDEN);
        }

        // Verify enrollment
        enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId())
                .orElseThrow(() -> new ApiException("You are not enrolled in this course", HttpStatus.FORBIDDEN));

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        
        Long previousLessonId = null;
        Long nextLessonId = null;

        for (int i = 0; i < lessons.size(); i++) {
            if (Objects.equals(lessons.get(i).getId(), lessonId)) {
                if (i > 0) {
                    previousLessonId = lessons.get(i - 1).getId();
                }
                if (i < lessons.size() - 1) {
                    nextLessonId = lessons.get(i + 1).getId();
                }
                break;
            }
        }

        LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElseGet(() -> {
                    LessonProgress newProgress = LessonProgress.builder()
                            .userId(user.getId())
                            .lessonId(lessonId)
                            .isCompleted(false)
                            .watchTimeSeconds(0)
                            .build();
                    return lessonProgressRepository.save(newProgress);
                });

        // Update last accessed time
        progress.setLastAccessedAt(LocalDateTime.now());
        lessonProgressRepository.save(progress);

        // Tính % tiến độ bài học
        double lessonPercent = 0.0;
        if (lesson.getDurationSeconds() != null && lesson.getDurationSeconds() > 0) {
            lessonPercent = Math.min(100.0,
                    (double) progress.getWatchTimeSeconds() / lesson.getDurationSeconds() * 100);
        }

        return LessonDetailResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .type(lesson.getType() != null ? lesson.getType().name() : null)
                .contentUrl(lesson.getContentUrl())
                .contentText(lesson.getContentText())
                .durationSeconds(lesson.getDurationSeconds())
                .orderIndex(lesson.getOrderIndex())
                .isCompleted(progress.getIsCompleted())
                .watchTimeSeconds(progress.getWatchTimeSeconds())
                .currentTimeSeconds(progress.getCurrentTimeSeconds())
                .lessonProgressPercent(Math.round(lessonPercent * 10.0) / 10.0)
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .previousLessonId(previousLessonId)
                .nextLessonId(nextLessonId)
                .build();
    }

    @Transactional(readOnly = true)
    public LessonTutorSummaryResponse summarizeLessonWithTutor(String email, Long lessonId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException("Lesson not found", HttpStatus.NOT_FOUND));

        Course course = courseRepository.findById(lesson.getCourseId())
                .orElseThrow(() -> new ApiException("Course not found", HttpStatus.NOT_FOUND));
        if (course.getStatus() == Course.Status.BLOCKED) {
            throw new ApiException("Khóa học đã bị khóa bởi quản trị viên", HttpStatus.FORBIDDEN);
        }

        enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId())
                .orElseThrow(() -> new ApiException("You are not enrolled in this course", HttpStatus.FORBIDDEN));

        SourcePayload sourcePayload = buildSummarySource(lesson, course);
        String summary = aiTutorService.summarize(new AISummarizeRequest(sourcePayload.content())).getSummary();

        return LessonTutorSummaryResponse.builder()
                .lessonId(lessonId)
                .summary(summary)
                .transcriptUsed(sourcePayload.transcriptUsed())
                .sourceType(sourcePayload.sourceType())
                .build();
    }

    private SourcePayload buildSummarySource(Lesson lesson, Course course) {
        if (lesson.getType() == Lesson.LessonType.VIDEO) {
            Optional<String> transcriptOpt = fetchYoutubeTranscript(lesson.getContentUrl());
            if (transcriptOpt.isPresent()) {
                String transcript = transcriptOpt.get();
                String content = String.format(
                        "Tóm tắt bài học tiếng Anh sau cho học viên:\n"
                                + "Course: %s\nLesson: %s\n"
                                + "Nguồn: YouTube transcript\n\n%s",
                        safe(course.getTitle()),
                        safe(lesson.getTitle()),
                        transcript);
                return new SourcePayload(trimMax(content), true, "youtube_transcript");
            }
        }

        if (lesson.getContentText() != null && !lesson.getContentText().isBlank()) {
            String content = String.format(
                    "Tóm tắt bài học tiếng Anh sau cho học viên:\nCourse: %s\nLesson: %s\n\n%s",
                    safe(course.getTitle()),
                    safe(lesson.getTitle()),
                    lesson.getContentText());
            return new SourcePayload(trimMax(content), false, "content_text");
        }

        String fallback = String.format(
                "Không có transcript/video text trực tiếp. Hãy tạo bản tóm tắt học tập ngắn gọn dựa trên metadata bài học:\n"
                        + "- Course: %s\n- Lesson: %s\n- Type: %s\n- URL: %s\n"
                        + "Yêu cầu: nêu mục tiêu học, điểm trọng tâm cần chú ý, cách tự ôn tập.",
                safe(course.getTitle()),
                safe(lesson.getTitle()),
                lesson.getType() != null ? lesson.getType().name() : "N/A",
                safe(lesson.getContentUrl()));
        return new SourcePayload(trimMax(fallback), false, "metadata_fallback");
    }

    private Optional<String> fetchYoutubeTranscript(String contentUrl) {
        String videoId = extractYoutubeVideoId(contentUrl);
        if (videoId == null) {
            return Optional.empty();
        }

        List<String> candidates = List.of(
                "https://www.youtube.com/api/timedtext?lang=vi&v=" + videoId,
                "https://www.youtube.com/api/timedtext?lang=en&v=" + videoId,
                "https://www.youtube.com/api/timedtext?lang=en&kind=asr&v=" + videoId,
                "https://www.youtube.com/api/timedtext?lang=vi&kind=asr&v=" + videoId);

        HttpClient client = HttpClient.newBuilder().build();
        for (String candidate : candidates) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(candidate))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                if (body == null || body.isBlank() || !body.contains("<text")) {
                    continue;
                }

                String transcript = extractTextFromTimedText(body);
                if (!transcript.isBlank()) {
                    return Optional.of(trimMax(transcript));
                }
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }

    private String extractYoutubeVideoId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Matcher matcher = YOUTUBE_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractTextFromTimedText(String xml) {
        Matcher matcher = YOUTUBE_TEXT_PATTERN.matcher(xml);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String raw = matcher.group(1);
            String plain = HtmlUtils.htmlUnescape(raw)
                    .replace('\n', ' ')
                    .replaceAll("\\s+", " ")
                    .trim();
            if (!plain.isBlank()) {
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(plain);
            }
        }
        return out.toString().trim();
    }

    private String trimMax(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= SUMMARY_MAX_CHARS) {
            return text;
        }
        return text.substring(0, SUMMARY_MAX_CHARS);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record SourcePayload(String content, boolean transcriptUsed, String sourceType) {
    }
}
