package dk.digitalidentity.sofd.dao.model;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classification")
@Audited
@Getter
@Setter
public class Classification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String identifier;

    @Column(nullable = false)
    private String name;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "classification", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassificationItem> items = new ArrayList<>();
}