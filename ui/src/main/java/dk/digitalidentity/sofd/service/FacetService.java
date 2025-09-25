package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.FacetDao;
import dk.digitalidentity.sofd.dao.model.Facet;

@Service
public class FacetService {

	@Autowired
	private FacetDao facetDao;
	
	public List<Facet> getAll() {
		return facetDao.findAll();
	}

	public Facet getById(long id) {
		return facetDao.findById(id);
	}

	public void delete(Facet facet) {
		facetDao.delete(facet);
	}

	public Facet save(Facet facet) {
		return facetDao.save(facet);
	}
}
