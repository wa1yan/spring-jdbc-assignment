package com.jdc.project.model.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Service;

import com.jdc.project.model.dto.Project;
import com.jdc.project.model.service.utils.ProjectHelper;

@Service
public class ProjectService {
	
	@Autowired
	private ProjectHelper projectHelper;
	
	@Autowired
	private SimpleJdbcInsert projectInsert;
	
	@Autowired
	private NamedParameterJdbcTemplate template;
	
	private RowMapper<Project> rowMapper;
	
	public ProjectService() {
		rowMapper = new BeanPropertyRowMapper<>(Project.class);
	}

	public int create(Project project) {
		
			projectHelper.validate(project);
			
			var params = projectHelper.insertParams(project);
			
			var result = projectInsert.executeAndReturnKey(params);
				
			return result.intValue();		
	}

	public Project findById(int id) {
		String sql = """
					select p.id, p.name, p.description, p.months, p.start as startDate,
					m.id as managerId, m.name as managerName, m.login_id as managerLogin
					from project p 
					inner join member m 
					on p.manager = m.id
					where p.id = :id
					""";
		return template.queryForObject(sql, Map.of("id", id), rowMapper);
	}

	public List<Project> search(String project, String manager, LocalDate dateFrom, LocalDate dateTo) {
		
		var sb = new StringBuffer("""
				select * from project p
				inner join member m on p.manager = m.id
				where 1 = 1
					""");
		
		var params = new HashMap<String, Object>();
		
		if (null != project) {
			params.put("project", project.concat("%"));
			sb.append("and lower(p.name) like lower(:project) ");
		}

		if (null != manager) {
			params.put("manager", manager.concat("%"));
			sb.append("and lower(m.name) like lower(:manager) ");
		}

		if (null != dateFrom) {
			params.put("dateFrom", Date.valueOf(dateFrom));
			if (null != dateTo) {
				params.put("dateTo", Date.valueOf(dateTo));
				sb.append("and p.start between :dateFrom and :dateTo ");
			} else {
				sb.append("and p.start >= :dateFrom ");
			}
		}

		if (null != dateTo) {
			if (null == dateFrom) {
				params.put("dateTo", Date.valueOf(dateTo));
				sb.append("and p.start <= :dateTo ");
			}
		}
		
		return template.query(sb.toString(), params, rowMapper);
	}

	public int update(int id, String name, String description, LocalDate startDate, int month) {
		String sql = """
				update project
				set name = :name , description = :description, start = :start, months = :months
				where id = :id
				""";
		var params = new HashMap<String, Object>();
		params.put("name", name);
		params.put("description", description);
		params.put("start", Date.valueOf(startDate));
		params.put("months", month);
		params.put("id", id);
		
		return template.update(sql, params);
	}

	public int deleteById(int id) {
		return template.update("delete from project where id = :id", Map.of("id",id));
	}
}
