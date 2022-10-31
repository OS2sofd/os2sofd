package dk.digitalidentity.sofd.service;

import dk.digitalidentity.sofd.dao.PhotoDao;
import dk.digitalidentity.sofd.dao.model.Photo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.Date;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

@Service
public class PhotoService {
    @Autowired
    private PhotoDao photoDao;

    @Transactional
    public void save(String personUuid, byte[] data) {
        Photo photo = photoDao.findByPersonUuid(personUuid);
        if (photo == null) {
            photo = new Photo();
            photo.setPersonUuid(personUuid);
        }
        photo.setLastChanged(new Date());
        photo.setData(data);
        photo.setChecksum(getCRC32Checksum(data));
        photo.setFormat(getImageFormat(data));
        photoDao.save(photo);
    }

    @Transactional
    public void delete(String personUuid) {
        photoDao.deleteByPersonUuid(personUuid);
    }

    private long getCRC32Checksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    private String getImageFormat(byte[] data) {
        try {
            String contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(data));
            return contentType.startsWith("image/") ? contentType.substring("image/".length()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}