package de.kjgstbarbara.security;

import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.PersonsRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Optional;

public class SecurityUtils {
    public static String generatePassword(int length) throws NoSuchAlgorithmException {
        final String chrs = "0123456789abcdefghijklmnopqrstuvwxyz-_ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        return secureRandom
                .ints(length, 0, chrs.length())
                .mapToObj(chrs::charAt)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    public static boolean checkPassword(String password) {
        return password.length() > 10;
        //return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(.{2,})");
    }

    public static boolean isGlobalAdmin(UserDetails userDetails) {
        for(GrantedAuthority authority : userDetails.getAuthorities()) {
            if(authority.getAuthority().equals("ADMIN")) {
                return true;
            }
        }
        return false;
    }

    public static Optional<Person> getPerson(UserDetails userDetails, PersonsRepository personsRepository) {

    }
}
