package eu.efti.eftigate.repository;

import eu.efti.eftigate.entity.PlatformEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlatformRepository extends JpaRepository<PlatformEntity, Long> {
    @Query("SELECT p.communicationType FROM PlatformEntity p where p.platformId = :platformId")
    String findCommunicationTypeById(String platformId);

    @Query("SELECT p.platformId FROM PlatformEntity p where p.clientId = :clientId")
    String findPlatformIdByClientId(String clientId);

    @Query("SELECT p.url FROM PlatformEntity p where p.platformId = :platformId")
    String findUrlById(String platformId);
}
