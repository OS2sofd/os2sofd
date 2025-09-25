package dk.digitalidentity.sofd.service;

import dk.digitalidentity.sofd.dao.ClientIpAddressesDao;
import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.ClientIpAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientIpAddressService {
    private final ClientIpAddressesDao clientIpAddressesDao;

    public void delete(ClientIpAddress clientIpAddress) {
        clientIpAddressesDao.delete(clientIpAddress);
    }

    public void deleteAll(List<ClientIpAddress> clientIpAddresses) {
        clientIpAddressesDao.deleteAll(clientIpAddresses);
    }

    public List<ClientIpAddress> getAll() {
        return clientIpAddressesDao.findAll();
    }

    public List<ClientIpAddress> getAllByClient(Client client) { return clientIpAddressesDao.findByClient(client); }

    public List<String> getAllIps() {
        return clientIpAddressesDao.findAll().stream().map(ClientIpAddress::getIp).toList();
    }

    public List<String> getAllIpsByClient(Client client) { return clientIpAddressesDao.findByClient(client).stream().map(ClientIpAddress::getIp).toList(); }

    public ClientIpAddress save(ClientIpAddress clientIpAddress) {
        return clientIpAddressesDao.save(clientIpAddress);
    }

    public List<ClientIpAddress> saveAll(List<ClientIpAddress> clientIpAddresses) {
        return clientIpAddressesDao.saveAll(clientIpAddresses);
    }
}