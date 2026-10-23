package com.exam.service.clazz;

import com.exam.common.exception.BizException;
import com.exam.common.page.PageQuery;
import com.exam.common.page.PageResult;
import com.exam.repository.entity.SchoolClass;
import com.exam.repository.mapper.SchoolClassMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 班级管理，作为 CRUD 通用模板的样板实现。
 *
 * <p>后续题库、试卷等模块照这个结构写：分页查询带排序、软删除、
 * 归档不影响历史成绩。这里把公共动作都做全，其他模块照抄即可。
 */
@Service
public class ClassService {

    private final SchoolClassMapper classMapper;

    public ClassService(SchoolClassMapper classMapper) {
        this.classMapper = classMapper;
    }

    /** 分页查询，支持按名称/科目模糊搜索，按创建时间倒序 */
    public PageResult<SchoolClass> page(PageQuery query, String keyword) {
        Page<SchoolClass> page = Page.of(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<SchoolClass> wrapper = new LambdaQueryWrapper<SchoolClass>()
                .like(hasText(keyword), SchoolClass::getName, keyword)
                .or(hasText(keyword))
                .like(hasText(keyword), SchoolClass::getSubject, keyword)
                .orderByDesc(SchoolClass::getCreatedAt);

        Page<SchoolClass> result = classMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    public SchoolClass getById(long id) {
        SchoolClass clazz = classMapper.selectById(id);
        if (clazz == null) {
            throw BizException.notFound("班级");
        }
        return clazz;
    }

    public List<SchoolClass> listAll() {
        return classMapper.selectList(null);
    }

    @Transactional
    public long create(String name, String subject, Long teacherId) {
        SchoolClass clazz = new SchoolClass();
        clazz.setName(name);
        clazz.setSubject(subject);
        clazz.setTeacherId(teacherId);
        clazz.setArchived(0);
        classMapper.insert(clazz);
        return clazz.getId();
    }

    @Transactional
    public void update(long id, String name, String subject, Long teacherId) {
        SchoolClass existing = getById(id);
        existing.setName(name);
        existing.setSubject(subject);
        existing.setTeacherId(teacherId);
        classMapper.updateById(existing);
    }

    /**
     * 软删除。成绩表仍引用 class_id，所以这里绝不能物理删。
     */
    @Transactional
    public void delete(long id) {
        getById(id);
        classMapper.deleteById(id);
    }

    /** 归档：不影响历史成绩，只是从默认列表里淡出 */
    @Transactional
    public void archive(long id, boolean archived) {
        SchoolClass existing = getById(id);
        existing.setArchived(archived ? 1 : 0);
        classMapper.updateById(existing);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
