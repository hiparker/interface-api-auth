package com.parker.api.producer.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Created Date by 2020/5/15 0015.
 *
 * 用户 Entity
 * @author Parker
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 登录名
     */
    private String userLoginName;

    /**
     * 密码
     */
    private String password;

}
