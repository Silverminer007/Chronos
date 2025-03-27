package de.chronoslive.controllers;

import de.chronoslive.entitys.Person;
import de.chronoslive.services.PersonsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;

@RestController
@RequestMapping("/profile-image/")
public class ProfileImageController {
    private final PersonsService personsService;

    public ProfileImageController(PersonsService personsService) {
        this.personsService = personsService;
    }

    @GetMapping(value = "/{personId}", produces = "image/png")
    public byte[] profileImage(@PathVariable long personId) {
        Path profileImagePath = Paths.get("profile-image/" + personId + ".png");
        if (Files.exists(profileImagePath)) {
            return loadProfileImageFromFileSystem(profileImagePath);
        }

        Optional<Person> personOptional = this.personsService.getPersonsRepository().findById(personId);

        if (personOptional.isEmpty()) {
            return loadDefaultProfileImage();
        }
        Person person = personOptional.get();

        // Extract old images from database
        String base64ProfileImage = person.getProfileImage();

        if(base64ProfileImage == null || base64ProfileImage.isBlank()) {
            return loadDefaultProfileImage();
        }

        byte[] profileImageBytes = Base64.getDecoder().decode(base64ProfileImage);
        try {
            // this way the amount of profile images in database decreases
            Files.createDirectories(profileImagePath.getParent());
            Files.write(profileImagePath, profileImageBytes);
            person.setProfileImage("");
            this.personsService.getPersonsRepository().save(person);
        } catch (IOException ignored) {
        }
        return profileImageBytes;
    }

    private byte[] loadProfileImageFromFileSystem(Path pathToProfileImage) {
        try {
            return Files.readAllBytes(pathToProfileImage);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private byte[] loadDefaultProfileImage() {
        try {
            try (InputStream inputStream = this.getClass().getClassLoader()
                    .getResourceAsStream("./default-profile-image.png")) {
                if(inputStream == null) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return inputStream.readAllBytes();
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
