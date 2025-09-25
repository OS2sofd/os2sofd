package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.ClientIpAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientIpAddressesDao extends JpaRepository<ClientIpAddress, Long> {
    List<ClientIpAddress> findByClient(Client client);
}
