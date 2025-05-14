package com.atme.ssyx.acl.service;

import com.atme.ssyx.model.acl.Admin;
import com.atme.ssyx.vo.acl.AdminQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AdminService extends IService<Admin> {

    IPage<Admin> selectAdminPage(Page<Admin> pageParam, AdminQueryVo adminQueryVo);
}
