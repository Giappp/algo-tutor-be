package org.rap.algotutorbe.learning.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.learning.enums.VideoProcessingStatus;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class VideoLesson extends Lesson {
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_object_key", length = 500)
    private String sourceObjectKey;

    @Column(name = "thumbnail_object_key", length = 500)
    private String thumbnailObjectKey;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private VideoProcessingStatus processingStatus = VideoProcessingStatus.PENDING_UPLOAD;
}
