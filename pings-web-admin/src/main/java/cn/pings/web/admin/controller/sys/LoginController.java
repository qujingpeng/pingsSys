package cn.pings.web.admin.controller.sys;

import cn.pings.commons.util.jwt.JwtUtil;
import cn.pings.jwt.verifier.JwtVerifier;
import cn.pings.service.api.sys.entity.Right;
import cn.pings.service.api.sys.entity.Role;
import cn.pings.service.api.sys.entity.User;
import cn.pings.sys.commons.util.ApiResponse;
import cn.pings.web.admin.controller.AbstractBaseController;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UnknownAccountException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 *********************************************************
 ** @desc  ： 登陆管理
 ** @author  Pings
 ** @date    2019/1/22
 ** @version v1.0
 * *******************************************************
 */
@RestController
@RequestMapping("/api/login")
public class LoginController extends AbstractBaseController {

    @Autowired
    private JwtVerifier verifier;

    /**
     *********************************************************
     ** @desc ： 登录
     ** @author Pings
     ** @date   2019/1/22
     ** @param  userName  用户名称
     ** @param  password  用户密码
     ** @return ApiResponse
     * *******************************************************
     */
    @ApiOperation(value="登录", notes="验证用户名和密码")
    @PostMapping(value = "/account")
    public ApiResponse account(String userName, String password, HttpServletResponse response){
        if(StringUtils.isBlank(userName) || StringUtils.isBlank(password))
            throw new UnknownAccountException("用户名/密码不能为空");

        //**md5加密
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        User user = this.userService.getByUserName(userName);
        if(user != null && user.getPassword().equals(password)) {
            JwtUtil.setHttpServletResponse(response, verifier.sign(userName));

            //**用户权限
            Set<String> rights = user.getRoles().stream().map(Role::getRights).flatMap(List::stream).map(Right::getCode).collect(toSet());
            return ApiResponse.success("登录成功", rights);
        } else
            return ApiResponse.failure("用户名/密码错误");
    }

    /**
     *********************************************************
     ** @desc ： 退出登录
     ** @author Pings
     ** @date   2019/3/26
     ** @return ApiResponse
     * *******************************************************
     */
    @ApiOperation(value="退出登录", notes="退出登录")
    @GetMapping(value = "/logout")
    public ApiResponse logout(){
        try{
            String userName = this.getCurrentUserName();
            this.verifier.invalidateSign(userName);
        } catch (Exception e){
            logger.warn("没有登录的用户访问退出登录");
        }

        //**退出登录
        SecurityUtils.getSubject().logout();

        return ApiResponse.success("退出登录成功");
    }
}
