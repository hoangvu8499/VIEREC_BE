package com.vierec.modules.course.entity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The videos of a lesson whose watching is tracked, with the keys stored in {@code lesson_progress.video_key}.
 * Must match {@code lessonVideos} / {@code youtubeVideoId} of the frontend: a video link that is not YouTube (Drive,
 * Vimeo...) cannot be embedded, so it is not tracked.
 */
public final class CourseVideos {

    private static final Pattern YOUTUBE_ID = Pattern.compile("^[\\w-]{11}$");
    private static final Pattern HOST_PREFIX = Pattern.compile("^(www|m|music)\\.");
    private static final List<String> PATH_SECTIONS = Arrays.asList("embed", "shorts", "live", "v");

    private CourseVideos() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    /** Keys of the tracked videos of the lesson, in display order (YouTube link first, then files). */
    public static List<String> keys(Lesson lesson) {
        List<String> keys = new ArrayList<>();
        String youtubeId = youtubeVideoId(lesson.getVideoUrl());
        if (youtubeId != null) {
            keys.add("youtube-" + youtubeId);
        }
        lesson.getLessonFiles().stream()
                .filter(file -> file.getFileType() == LessonFileType.VIDEO)
                .forEach(file -> keys.add("file-" + file.getFile().getId()));
        return keys;
    }

    /** YouTube video id of a {@code watch?v=}, {@code youtu.be/}, {@code /embed/}, {@code /shorts/}... link. */
    public static String youtubeVideoId(String url) {
        if (url == null) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            return null;
        }
        if (uri.getHost() == null || uri.getScheme() == null) {
            return null;
        }
        String host = HOST_PREFIX.matcher(uri.getHost().toLowerCase()).replaceFirst("");
        List<String> path = uri.getRawPath() == null ? Collections.<String>emptyList()
                : Arrays.asList(uri.getRawPath().split("/", -1));
        String id = null;
        if ("youtu.be".equals(host)) {
            id = path.size() > 1 ? path.get(1) : null;
        } else if ("youtube.com".equals(host) || "youtube-nocookie.com".equals(host)) {
            String section = path.size() > 1 ? path.get(1) : null;
            if ("watch".equals(section)) {
                id = queryParam(uri.getRawQuery(), "v");
            } else if (section != null && PATH_SECTIONS.contains(section)) {
                id = path.size() > 2 ? path.get(2) : null;
            }
        }
        return id != null && YOUTUBE_ID.matcher(id).matches() ? id : null;
    }

    private static String queryParam(String query, String name) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            if (equals > 0 && pair.substring(0, equals).equals(name)) {
                return pair.substring(equals + 1);
            }
        }
        return null;
    }
}
