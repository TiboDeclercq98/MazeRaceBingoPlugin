package com.mazebingo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

class SoundGenerator {

    private static final Logger log = LoggerFactory.getLogger(SoundGenerator.class);

    static InputStream generate(MazeSound sound) {
        switch (sound) {
            case SHORT_DOG_BARK: return loadWav("/com/mazebingo/sounds/short-dog-bark.wav");
            case WHIP:            return loadWav("/com/mazebingo/sounds/whip.wav");
            case BOBER:           return loadWav("/com/mazebingo/sounds/bober_kurwa.wav");
            case SAD_SOUND:     return loadWav("/com/mazebingo/sounds/sad_sound.wav");
            default:             return null;
        }
    }

    private static InputStream loadWav(String resource) {
        try (InputStream in = SoundGenerator.class.getResourceAsStream(resource)) {
            if (in == null) {
                log.warn("Sound resource not found on classpath: {}", resource);
                return null;
            }
            return new ByteArrayInputStream(in.readAllBytes());
        } catch (IOException e) {
            log.warn("Failed to read sound resource {}", resource, e);
            return null;
        }
    }

}
