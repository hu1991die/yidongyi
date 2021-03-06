package com.sectong.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sectong.domain.User;
import com.sectong.domain.UserCreateForm;
import com.sectong.message.Message;
import com.sectong.repository.UserRepository;
import com.sectong.service.UserService;
import com.sectong.validator.UserCreateFormValidator;

/**
 * 处理用户类接口
 * 
 * @author jiekechoo
 *
 */
@RestController
@PropertySource("classpath:message.properties")
@RequestMapping(value = "/api/v1/", name = "用户API")
public class UserController {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
	private UserService userService;
	private UserCreateFormValidator userCreateFormValidator;
	private UserRepository userRepository;

	private Message message = new Message();

	@Autowired
	private Environment env;

	@Autowired
	public UserController(UserService userService, UserCreateFormValidator userCreateFormValidator,
			UserRepository userRepository) {
		this.userService = userService;
		this.userCreateFormValidator = userCreateFormValidator;
		this.userRepository = userRepository;
	}

	/**
	 * 创建用户验证表单
	 * 
	 * @param binder
	 */
	@InitBinder("userCreateForm")
	public void initBinder(WebDataBinder binder) {
		binder.addValidators(userCreateFormValidator);
	}

	/**
	 * APP登录用接口
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "i/userLogin", method = RequestMethod.POST)
	public ResponseEntity<Message> userLogin(HttpServletRequest request) {
		User user = userService.getCurrentUser();
		if(user == null){
			message.setMsg(0, "用户登录失败");
			return new ResponseEntity<Message>(message, HttpStatus.OK);
		}else {
			message.setMsg(1, "用户登录成功", user);
			return new ResponseEntity<Message>(message, HttpStatus.OK);
		}
		
	}

	/**
	 * 创建用户接口
	 * 
	 * @param form
	 * @param bindingResult
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "create", method = RequestMethod.POST)
	public ResponseEntity<Message> handleUserCreateForm(@Valid @RequestBody UserCreateForm form,
			BindingResult bindingResult) {
		LOGGER.debug("Processing user create form={}, bindingResult={}", form, bindingResult);
		if (bindingResult.hasErrors()) {
			// failed validation
			message.setMsg(0, "user_create error: failed validation");
			return new ResponseEntity<Message>(message, HttpStatus.OK);
		}
		try {
			userService.create(form);
		} catch (DataIntegrityViolationException e) {
			LOGGER.warn("Exception occurred when trying to save the user, assuming duplicate username", e);
			bindingResult.reject("username.exists", "username already exists");
			message.setMsg(0, "创建用户失败：用户名已存在");
			return new ResponseEntity<Message>(message, HttpStatus.OK);

		}
		// ok, redirect
		message.setMsg(1, "create user success");
		return new ResponseEntity<Message>(message, HttpStatus.OK);
	}

	/**
	 * 使用 ResponseBody作为结果 200
	 * 
	 * @param id
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "i/user/{id}", method = RequestMethod.GET)
	public ResponseEntity<Message> findByUserId(@PathVariable long id) {
		User user = userRepository.findOne(id);
		HttpStatus status = user != null ? HttpStatus.OK : HttpStatus.NOT_FOUND;

		if (user == null) {
			message.setMsg(0, "未找到用户");
		} else {
			message.setMsg(1, "用户信息", user);
		}
		return new ResponseEntity<Message>(message, status);
	}

	/**
	 * 上传用户头像
	 * 
	 * @param file
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "i/uploadImage", method = RequestMethod.POST)
	public ResponseEntity<?> uploadImage(@RequestParam MultipartFile file, HttpServletRequest request) {
		message.setMsg(1, "用户上传头像成功", userService.uploadImage(file, request));
		return new ResponseEntity<Message>(message, HttpStatus.OK);

	}

	/**
	 * 获取用户列表
	 * 
	 * @param current
	 * @param rowCount
	 * @param searchPhrase
	 * @return
	 */
	@RequestMapping(value = "getUserList", method = RequestMethod.POST)
	public Object getUserList(@RequestParam(required = false) int current, @RequestParam(required = false) int rowCount,
			@RequestParam(required = false) String searchPhrase) {
		return userService.getUserList(current, rowCount, searchPhrase);

	}

}
