package dk.digitalidentity.sofd.dao.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

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
