package self.izouir.bitkionline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.SupportMessage;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
}
