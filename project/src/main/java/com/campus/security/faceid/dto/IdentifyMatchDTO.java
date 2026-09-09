package com.campus.security.faceid.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single face match, in the wire shape shared by /incidents/upload and /incidents/identify.
 * Field names/types match the Android app's IdentifyMatch model exactly (personId as a String,
 * snake_case keys) since that contract is fixed on the client side; studentClass and faceBox are
 * additive extras the admin web dashboard uses that the Android client simply ignores.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IdentifyMatchDTO {
    @JsonProperty("person_id")
    private String personId;

    private String name;

    private Double confidence;

    @JsonProperty("photo_url")
    private String photoUrl;

    @JsonProperty("student_class")
    private String studentClass;

    @JsonProperty("face_box")
    private FaceBoxDTO faceBox;

    public static IdentifyMatchDTO from(MatchResultDTO m) {
        return IdentifyMatchDTO.builder()
                .personId(m.getMatchedStudentId() != null ? String.valueOf(m.getMatchedStudentId()) : null)
                .name(m.getMatchedStudentName())
                .confidence(m.getConfidenceScore())
                .photoUrl(null)
                .studentClass(m.getMatchedStudentClass())
                .faceBox(m.getFaceBox())
                .build();
    }
}
