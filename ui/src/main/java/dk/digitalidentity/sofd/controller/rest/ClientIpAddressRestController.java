package dk.digitalidentity.sofd.controller.rest;

import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.ClientIpAddress;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.ClientService;
import dk.digitalidentity.sofd.service.ClientIpAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RequireAdminAccess
@RequiredArgsConstructor
@RestController
public class ClientIpAddressRestController {
    private final ClientService clientService;
    private final ClientIpAddressService clientIpAddressService;

    @PostMapping("/ui/client/clientIpAddresses/save/{clientId}")
    public ResponseEntity<?> saveIpAddresss(@RequestBody List<String> ipRanges, @PathVariable long clientId) {
        Client client = clientService.getClientById(clientId);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        
        List<ClientIpAddress> ipsToBeSaved = new ArrayList<>();
        List<String> badIp = new ArrayList<>();
        List<ClientIpAddress> currentIps = clientIpAddressService.getAllByClient(client);
        
        for (String ipRange : ipRanges) {
            //validates whether the input is valid v4 og v6 ip address
            //would have been trivial to make regex ipv4, but not ipv6
            //and this way it uses same functions to validate input and ip addresses
            try {
            	// force throw exception on error
            	new IpAddressMatcher(ipRange);
            	
                ClientIpAddress ipAddress = currentIps.stream().filter(clientIpAddress -> clientIpAddress.getIp().equals(ipRange)).findFirst().orElse(null);
                if(ipAddress != null) {
                    currentIps.remove(ipAddress);
                }
                else {
                    ipAddress = new ClientIpAddress();
                    ipAddress.setIp(ipRange);
                    ipAddress.setClient(client);
                    ipsToBeSaved.add(ipAddress);
                }
            }
            catch (Exception e) {
                badIp.add(ipRange);
            }
        }

        if (!badIp.isEmpty()) {
            return ResponseEntity.badRequest().body(badIp.toString());
        }
        
        client.removeIpAddresses(currentIps);
        client.addIpAddresses(ipsToBeSaved);
        clientService.save(client);

        return ResponseEntity.ok().build();
    }
}
