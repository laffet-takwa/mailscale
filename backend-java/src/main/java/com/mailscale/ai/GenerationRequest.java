package com.mailscale.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GenerationRequest(
        @NotBlank @Size(max = 500) String brief,
        @Size(max = 200) String product,
        @Size(max = 200) String audience,
        Tone tone,
        @Max(5) int variantCount,
        boolean includeCtas) {

    public GenerationRequest {
        if (variantCount < 1) {
            variantCount = 1;
        }
        if (tone == null) {
            tone = Tone.PROFESSIONAL;
        }
    }

    public enum Tone {
        PROFESSIONAL, FRIENDLY, URGENT, MINIMAL
    }
}
