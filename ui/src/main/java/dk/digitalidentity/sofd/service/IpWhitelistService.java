package dk.digitalidentity.sofd.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.model.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service("ipWhitelistService")
@Slf4j
public class IpWhitelistService {
    private final HttpServletRequest request;

    public boolean isWhitelisted(Client client) {
        if(client.isInternal()) {
            return true;
        }

        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        
        List<IpAddressMatcher> allowList = client.getIpAddresses().stream().map(clientIpAddress -> new IpAddressMatcher(clientIpAddress.getIp())).toList();
        if (allowList.isEmpty()) {
            return true;
        }

        for (IpAddressMatcher matcher : allowList) {
            if(matcher.matches(ipAddress)) {
                return true;
            }
        }
        
        log.warn(ipAddress + " not whitelisted for apiKey: " + client.getName());

        return false;
    }
}