package dk.digitalidentity.sofd.service.xls;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Organisation;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.OrganisationService;
import dk.digitalidentity.sofd.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class HistoricalReportsOnOrgUnitsService {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private OrgUnitService orgUnitService;

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private SofdConfiguration configuration;

    private Locale locale = new Locale("da-DK");

    private Workbook buildExcelDocument(List<OrgUnitExcelDTO> orgUnits) {
        Workbook workbook = new XSSFWorkbook();

        //SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        // create excel xls sheet
        Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.historicreportorgunits.sheetname", null, locale));

        // create header row
        createHeader(workbook, sheet);

        // Create data cells
        int rowCount = 1;
        for (OrgUnitExcelDTO orgUnitDTO : orgUnits) {
            Row courseRow = sheet.createRow(rowCount++);

            courseRow.createCell(0).setCellValue(orgUnitDTO.getOrganisation());
            courseRow.createCell(1).setCellValue(orgUnitDTO.getUuid());
            courseRow.createCell(2).setCellValue(orgUnitDTO.getParentUuid());
            courseRow.createCell(3).setCellValue(orgUnitDTO.getName());
            courseRow.createCell(4).setCellValue(orgUnitDTO.getType());
            courseRow.createCell(5).setCellValue(orgUnitDTO.getAddress());
            courseRow.createCell(6).setCellValue(orgUnitDTO.getCvr());
            courseRow.createCell(7).setCellValue(orgUnitDTO.getEan());
            courseRow.createCell(8).setCellValue(orgUnitDTO.getSenr());
            courseRow.createCell(9).setCellValue(orgUnitDTO.getPnr());
            courseRow.createCell(10).setCellValue(orgUnitDTO.getPath());
        }

        format(sheet);
        return workbook;
    }

    private void format(Sheet sheet) {
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);
        sheet.autoSizeColumn(8);
        sheet.autoSizeColumn(9);
        sheet.autoSizeColumn(10);
        sheet.autoSizeColumn(11);
    }

    private void createHeader(Workbook workbook, Sheet sheet) {
        String header1 = messageSource.getMessage("xls.historicreportorgunits.orgunit.organisation", null, locale);
        String header2 = messageSource.getMessage("xls.historicreportorgunits.orgunit.uuid", null, locale);
        String header3 = messageSource.getMessage("xls.historicreportorgunits.orgunit.parentUuid", null, locale);
        String header4 = messageSource.getMessage("xls.historicreportorgunits.orgunit.name", null, locale);
        String header5 = messageSource.getMessage("xls.historicreportorgunits.orgunit.type", null, locale);
        String header6 = messageSource.getMessage("xls.historicreportorgunits.orgunit.address", null, locale);
        String header7 = messageSource.getMessage("xls.historicreportorgunits.orgunit.cvr", null, locale);
        String header8 = messageSource.getMessage("xls.historicreportorgunits.orgunit.ean", null, locale);
        String header9 = messageSource.getMessage("xls.historicreportorgunits.orgunit.senr", null, locale);
        String header10 = messageSource.getMessage("xls.historicreportorgunits.orgunit.pnr", null, locale);
        String header11 = messageSource.getMessage("xls.historicreportorgunits.orgunit.path", null, locale);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        Row header = sheet.createRow(0);
        createCell(header, 0, header1, headerStyle);
        createCell(header, 1, header2, headerStyle);
        createCell(header, 2, header3, headerStyle);
        createCell(header, 3, header4, headerStyle);
        createCell(header, 4, header5, headerStyle);
        createCell(header, 5, header6, headerStyle);
        createCell(header, 6, header7, headerStyle);
        createCell(header, 7, header8, headerStyle);
        createCell(header, 8, header9, headerStyle);
        createCell(header, 9, header10, headerStyle);
        createCell(header, 10, header11, headerStyle);
    }

    private static void createCell(Row header, int column, String value, CellStyle style) {
        Cell cell = header.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    @Transactional
    public void generateOrgunitReport() {
        List<Organisation> organisations = organisationService.getAll();

        List<OrgUnit> allOrgUnits = new ArrayList<OrgUnit>();
        for (Organisation organisation : organisations) {
            allOrgUnits.addAll(orgUnitService.getAll(organisation));
        }

        List<OrgUnitExcelDTO> orgUnitDTOs = allOrgUnits.stream().map(ou -> new OrgUnitExcelDTO(ou)).collect(Collectors.toList());

        Comparator<OrgUnitExcelDTO> comparator = Comparator.comparing(ou -> ou.getOrganisation());
        comparator = comparator.thenComparing(Comparator.comparing(ou -> ou.getPath()));

        // Sort
        Stream<OrgUnitExcelDTO> ouStream = orgUnitDTOs.stream().sorted(comparator);
        List<OrgUnitExcelDTO> sortedOrgUnits = ouStream.collect(Collectors.toList());

        // The S3 config
        String bucket = configuration.getS3().getBucketName();
        String path = configuration.getS3().getHistoricalReportsPath();

        Workbook workbook = buildExcelDocument(sortedOrgUnits);

        String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " SOFD Organisation.xlsx";

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            workbook.write(outputStream);
            workbook.close();

            s3Service.upload(bucket, path + "/" + filename, outputStream.toByteArray());
        } catch (FileNotFoundException e) {
            log.warn("HistoricalReportsOnOrgUnitsTask: File not found. ", e);
        } catch (IOException e) {
            log.warn("HistoricalReportsOnOrgUnitsTask: IO problem.", e);
        }
    }
}