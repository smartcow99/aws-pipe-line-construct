package com.ce.fisa.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ce.fisa.model.entity.Emp;


@Repository
public interface EmpRepository extends JpaRepository<Emp, Integer>{

	public Emp findByEname(String ename);	
	
	@Query("select e.ename, e.sal from Emp e where e.deptno=:deptno")
	public List<Object[]> findByDeptno(@Param("deptno") int deptno);
		
	
	@Modifying
	@Query("update Emp e set e.deptno=:deptno where e.empno=:empno")
	public int updateByEmpnoDeptno(@Param("empno") int empno, 
								   @Param("deptno") int deptno);
}
