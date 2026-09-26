package com.vierec.modules.course;

import com.vierec.modules.course.entity.Course;
import com.vierec.modules.course.entity.CourseEnrollment;
import com.vierec.modules.course.entity.CourseStatus;
import com.vierec.modules.course.entity.EnrollmentStatus;
import com.vierec.modules.course.entity.Lesson;
import com.vierec.modules.course.entity.LessonFile;
import com.vierec.modules.course.entity.LessonFileType;
import com.vierec.modules.file.entity.StoredFile;
import com.vierec.modules.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persists and reloads the course / lesson / file / enrollment entities to check their mappings.
 * Runs on H2 only; whether the entities match the real MySQL schema is checked by {@code ddl-auto: validate}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseEntityMappingTest {

    @Autowired
    private EntityManager em;

    @Test
    void persistsCourseWithOrderedLessonsFilesAndEnrollment() {
        User instructor = user("giangvien");
        User trainee = user("hocvien");

        Course course = new Course();
        course.setName("Phòng cháy chữa cháy cơ bản");
        course.setDescription(repeat("Mô tả dài. ", 100));
        course.setInstructor(instructor);
        course.setCreatedBy(instructor);
        em.persist(course);

        Lesson second = lesson(course, "Bài 2", 2);
        Lesson first = lesson(course, "Bài 1", 1);

        StoredFile pdf = file("https://cdn.vierec.com/bai1.pdf");
        StoredFile video = file("https://cdn.vierec.com/bai1.mp4");
        first.getLessonFiles().add(new LessonFile(first, video, LessonFileType.VIDEO, 2));
        first.getLessonFiles().add(new LessonFile(first, pdf, LessonFileType.DOCUMENT, 1));

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setUser(trainee);
        enrollment.setCourse(course);
        em.persist(enrollment);

        em.flush();
        em.clear();

        Course loaded = em.find(Course.class, course.getId());
        assertThat(loaded.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(loaded.getDescription()).hasSize(course.getDescription().length());
        assertThat(loaded.getInstructor().getUsername()).isEqualTo("giangvien");
        assertThat(loaded.getCreatedBy().getUsername()).isEqualTo("giangvien");
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getLessons()).extracting(Lesson::getTitle).containsExactly("Bài 1", "Bài 2");

        Lesson loadedFirst = loaded.getLessons().get(0);
        assertThat(loadedFirst.getLessonFiles().stream().map(lf -> lf.getFile().getUrl()).collect(Collectors.toList()))
                .containsExactly("https://cdn.vierec.com/bai1.pdf", "https://cdn.vierec.com/bai1.mp4");
        assertThat(loadedFirst.getLessonFiles().get(0).getId().getLessonId()).isEqualTo(first.getId());

        CourseEnrollment loadedEnrollment = em.find(CourseEnrollment.class, enrollment.getId());
        assertThat(loadedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(loadedEnrollment.getEnrolledAt()).isNotNull();
        assertThat(loadedEnrollment.getUpdatedAt()).isNotNull();
        assertThat(second.getId()).isNotNull();
    }

    @Test
    void softDeletedLessonsAreHiddenFromCourse() {
        Course course = course("Khoá học");
        lesson(course, "Còn", 1);
        Lesson removed = lesson(course, "Đã xoá", 2);
        removed.setDeletedAt(LocalDateTime.now());
        em.flush();
        em.clear();

        assertThat(em.find(Course.class, course.getId()).getLessons()).extracting(Lesson::getTitle)
                .containsExactly("Còn");
    }

    @Test
    void removingLessonFileDeletesLinkButKeepsFile() {
        Course course = course("Khoá học");
        Lesson lesson = lesson(course, "Bài", 1);
        StoredFile file = file("https://cdn.vierec.com/a.pdf");
        lesson.getLessonFiles().add(new LessonFile(lesson, file, LessonFileType.DOCUMENT, 0));
        em.flush();

        lesson.getLessonFiles().clear();
        em.flush();
        em.clear();

        assertThat(em.find(Lesson.class, lesson.getId()).getLessonFiles()).isEmpty();
        assertThat(em.find(StoredFile.class, file.getId())).isNotNull();
    }

    @Test
    void userCannotEnrollTwiceInSameCourse() {
        User trainee = user("hocvien");
        Course course = course("Khoá học");
        enroll(trainee, course);
        em.flush();

        // IDENTITY ids make Hibernate insert on persist(), so the unique key fails right there.
        assertThatThrownBy(() -> enroll(trainee, course)).isInstanceOf(PersistenceException.class);
    }

    // ------------------------------------------------------------------ helpers

    private User user(String username) {
        User user = User.builder()
                .username(username)
                .passwordHash("$2a$12$hash")
                .firstName("Tên")
                .lastName("Họ")
                .build();
        em.persist(user);
        return user;
    }

    private Course course(String name) {
        Course course = new Course();
        course.setName(name);
        em.persist(course);
        return course;
    }

    private Lesson lesson(Course course, String title, int sortOrder) {
        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setTitle(title);
        lesson.setSortOrder(sortOrder);
        em.persist(lesson);
        return lesson;
    }

    private StoredFile file(String url) {
        StoredFile file = new StoredFile();
        file.setUrl(url);
        file.setOriginalName(url.substring(url.lastIndexOf('/') + 1));
        file.setContentType("application/octet-stream");
        file.setSizeBytes(1L);
        em.persist(file);
        return file;
    }

    private void enroll(User user, Course course) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        em.persist(enrollment);
    }

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
