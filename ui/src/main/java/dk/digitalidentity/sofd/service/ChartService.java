package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.OrganisationsdiagramDao;
import dk.digitalidentity.sofd.dao.model.Chart;

@Service
public class ChartService {

	@Autowired
	private OrganisationsdiagramDao organisationsdiagramDao;

	public List<Chart> getAll() {
		return organisationsdiagramDao.findAll();
	}

	public Chart findById(long id) {
		return organisationsdiagramDao.findById(id);
	}

	public Chart findByUuid(String uuid) {
		return organisationsdiagramDao.findByUuid(uuid);
	}

	public Chart save(Chart entity) {
		return organisationsdiagramDao.save(entity);
	}

	public void delete(Chart orgDiagram) {
		organisationsdiagramDao.delete(orgDiagram);
	}

	public String getDefaultStyle() {
		return "/*Farven på linjerne. De burde alle sættes til samme farve*/\r\n"
                + ".orgchart ul li ul li .node::before {\r\n"
                + "    background-color: #656565;\r\n"
                + "}\r\n"
                + ".orgchart .hierarchy::before {\r\n"
                + "    border-color: #656565;\r\n"
                + "}\r\n"
                + ".orgchart .node:not(:only-child)::after {\r\n"
                + "    background-color: #656565;\r\n"
                + "}\r\n"
                + ".orgchart .nodes.vertical .hierarchy::after, .orgchart .nodes.vertical .hierarchy::before {\r\n"
                + "    border-color:  #656565;\r\n"
                + "}\r\n"
                + ".orgchart .nodes.vertical::before {\r\n"
                + "    background-color: #656565;\r\n"
                + "}\r\n"
                + "\r\n"
                + "/*Boksen, hvor navnet på enheden står*/\r\n"
                + ".orgchart .node .title {\r\n"
                + "    background-color: #4765a0;\r\n"
				+ "    color: #ffffff;\r\n"
                + "    font-size: x-small;\r\n"
				+ "    display: table-cell;\r\n"
				+ "    vertical-align: middle;\r\n"
				+ "    white-space: break-spaces;\r\n"
                + "}\r\n"
                + "\r\n"
                + "/*Boksen hvor leders navn står, hvis leder skal vises*/\r\n"
                + ".orgchart .node .content {\r\n"
                + "    border: 1px solid #4765a0;\r\n"
                + "    background-color: white;\r\n"
                + "    color: black;\r\n"
                + "    font-size: x-small;\r\n"
                + "}\r\n"
				+ "/*Boksen hvor leders navn står hvis leder er nedarvet*/\r\n"
				+ ".inheritedManager {\r\n"
				+ " background-color: #F0F0F0 !important;\r\n"
				+ "}\r\n"
                + "\r\n"
                + "/*Farven på rammen rundt om boksen, hvis den er valgt*/\r\n"
                + ".orgchart .node.focused {\r\n"
                + "    background-color: #b9c1c580;\r\n"
                + "}\r\n"
                + "\r\n"
                + "/*Farven på rammen rundt om boksen, hvis man kører musen over den*/\r\n"
                + ".orgchart ul li .node:hover {\r\n"
                + "    background-color: #b9c1c580;\r\n"
                + "}\r\n"
                + "\r\n"
                + "/*Hele diagrammets baggrund*/\r\n"
                + ".orgchart {\r\n"
                + "    background-image: none;\r\n"
                + "}";
	}

}
