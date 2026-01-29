package dk.digitalidentity.sofd.dao.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "batch_job_execution")
public class BatchJobExecution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
	@Column
    private String jobName;

    @Column
    private Date lastExecutionTime;

    @Column 
    private long errorCount;

    @Column
    private Date lastErrorTime;
}
