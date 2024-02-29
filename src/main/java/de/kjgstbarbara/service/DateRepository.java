package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Date;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DateRepository extends JpaRepository<Date, Long> {
}
