package injectLog.aspect;

import injectLog.util.AspectUtils;
import java.util.UUID;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;

@Aspect()
public abstract class LogInjectAspect {

	@Pointcut
	public abstract void monitorSQL();

	@Pointcut
	public abstract void restCall();
	
	@Pointcut
	public abstract void methodCall();

	private UUID id = null;
	
	/**
	 * need | after tag
	 */
	String TAG = "INJECT | ";
	String BEFORE_REST = "Before Rest | ";
	String AFTER_REST = "After Rest | ";
	String CALLED = "Called | ";
	String SQL = "Sql | ";

	@Around("restCall()")
	public Object restCall(ProceedingJoinPoint jp) throws Throwable {
		id = UUID.randomUUID();
		String functionCall = getFunctionCallName(jp);
		String currentTime = getCurrentTime();
		System.out.println(currentTime + TAG + BEFORE_REST + jp.getSignature().toString() + " | @id:" + id);
		long startTime = System.nanoTime();
		Object result = jp.proceed();
		System.out.println(currentTime + TAG + AFTER_REST + jp.getSignature().toString() + " | executed in: " + (System.nanoTime() - startTime) + "ns" + " | @id:" + id );
		return result;
	}
	
	@Before("methodCall()")
	public void methodCall(JoinPoint jp) throws Throwable {
		String currentTime = getCurrentTime();
		System.out.println(currentTime + TAG +  CALLED + jp.getSignature().toString() + " | @id:" + id);
	}

	@Around("monitorSQL()")
	public Object monitorSQL(ProceedingJoinPoint jp) throws Throwable {
		long startTime = System.nanoTime();
		Object result = jp.proceed();
		String currentTime = getCurrentTime();
		System.out.println(currentTime + TAG + SQL + jp.getArgs()[0] + " | executed in: " + (System.nanoTime() - startTime)
				+ "ns | " + "@id:" + id);
		return result;
	}

	/**
	 * Helper function for generating the executed function signature Example:
	 * org.springframework.samples.petclinic.web.OwnerController.java/processUpdateOwnerForm/OwnerBindingResultSessionStatus|org.springframework.samples.petclinic.web.OwnerController @id:19a7b5ba-4431-4a96-bfb6-a2b1014c5216
	 * OwnerController.java is the class name
	 * processUpdateOwnerForm is the method name
	 * OwnerBindingResultSessionStatus is the first parameter type
	 * OwnerController is the final class name
	 * @param jp
	 * @return
	 */
	private String getFunctionCallName(ProceedingJoinPoint jp) {
		Signature signature = jp.getSignature();
		String currentTime = getCurrentTime();

		String parmTypes = "";
		if (signature instanceof CodeSignature) {
			final CodeSignature ms = (CodeSignature) signature;
			final Class<?>[] parameterTypes = ms.getParameterTypes();
			for (final Class<?> pt : parameterTypes) {
				// System.out.println("Parameter type:" + pt.getName());
				parmTypes += pt.getName().split("\\.")[pt.getName().split("\\.").length - 1];
			}

		}

		String functionCall = null;
		try {
			System.out.println(currentTime + TAG + "debug info | jp=" + jp);
			if (jp.getSignature().getDeclaringType().getName() == null) {
				System.out.println(currentTime + TAG + "debug info | jp.getSignature().getDeclaringType().getName() == null");
			}
			
			if (jp.getSignature().getName() == null) {
				System.out.println(currentTime + TAG + "debug info | jp.getSignature().getName() == null");
			}
			
			if (parmTypes == "") {
				System.out.println(currentTime + TAG + "debug info | parmTypes == null");
			}
			
			if (jp.getTarget() == null) {
				System.out.println(currentTime + TAG + "debug info | jp.getTarget() == null,jp=" + jp);
			}
			
			if (jp.getTarget().getClass() == null) {
				System.out.println(currentTime + TAG + "debug info | jp.getTarget() == null,jp=" + jp.getTarget());
			}
			
			
			if (jp.getTarget().getClass().getName() == null) {
				System.out.println(currentTime + TAG + "debug info | jp.getTarget().getClass().getName() == null");
			}
			
			
			if (parmTypes == null) {
				functionCall = jp.getSignature().getDeclaringType().getName() + ".java/" + jp.getSignature().getName() + "/"
						+ "no parameter" + " | " + jp.getTarget().getClass().getName();
			} else {
				functionCall = jp.getSignature().getDeclaringType().getName() + ".java/" + jp.getSignature().getName() + "/"
						+ parmTypes + " | " + jp.getTarget().getClass().getName();
			}
			
			// + System.identityHashCode(jp.getTarget())
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return functionCall;
	}

	/**
	 * Helper function for debugging pointcuts
	 */
	private void printStackTraces() {
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			System.out.println(ste);
		}
	}
	
	private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return sdf.format(date) + "| ";
	}
}
