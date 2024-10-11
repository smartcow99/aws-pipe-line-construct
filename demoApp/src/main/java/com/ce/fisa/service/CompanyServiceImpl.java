package com.ce.fisa.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ce.fisa.dao.DeptRepository;
import com.ce.fisa.dao.EmpRepository;
import com.ce.fisa.exception.NotExistEmpException;
import com.ce.fisa.model.domain.dto.DeptDTO;
import com.ce.fisa.model.domain.dto.EmpDTO;
import com.ce.fisa.model.entity.Dept;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;


@Slf4j  
@Service
public class CompanyServiceImpl implements CompanyService{
	
	@Autowired
	private EmpRepository empDao;
	@Autowired
	private DeptRepository deptDao;

	private ModelMapper mapper = new ModelMapper();

	@Override
	public EmpDTO getEmp(String ename) throws NotExistEmpException {
		EmpDTO emp = mapper.map(empDao.findByEname(ename), EmpDTO.class);
		System.out.println(emp);
		return emp;
	}

	
	@Override
	public List<Object[]> getEmpsbyDeptno(int deptno) {
		List<Object[]> emps = empDao.findByDeptno(deptno);
		return emps;
	}

	//사번으로 부서 번호 수정하는 메소드
	@Override
	@Transactional
	public boolean updateDeptnoByEmpno(int empno, int newDeptno) throws NotExistEmpException {
		int result = empDao.updateByEmpnoDeptno(empno, newDeptno);
		if(result != 1) {
			throw new NotExistEmpException("사번에 일치되는 사원이 없습니다");
		}
		return true;
	}


	@Override
	public List<DeptDTO> getDeptAll() {
	    List<Dept> deptEntities = deptDao.findAll();
	    System.out.println(deptEntities);
	     // 유틸리티 메서드로 리스트 매핑 처리
        return mapList(deptEntities, DeptDTO.class);
    }

    // 리스트 매핑을 위한 유틸리티 메서드
    private <D, T> List<D> mapList(List<T> entityList, Class<D> outClass) {
        return entityList.stream()
                .map(entity -> mapper.map(entity, outClass))
                .toList();
    }
	
}
