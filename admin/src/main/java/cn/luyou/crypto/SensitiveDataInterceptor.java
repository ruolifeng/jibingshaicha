package cn.luyou.crypto;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

/**
 * 写入前加密、读出后解密，并把 WHERE 中的敏感条件改成密文等值比较。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {org.apache.ibatis.mapping.MappedStatement.class, Object.class}),
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
public class SensitiveDataInterceptor implements Interceptor {

    private final FieldEncryptor encryptor;

    public SensitiveDataInterceptor(FieldEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String method = invocation.getMethod().getName();
        if ("update".equals(method)) {
            List<Runnable> restores = SensitiveEntities.encryptParameter(invocation.getArgs()[1], encryptor);
            try {
                return invocation.proceed();
            } finally {
                for (int i = restores.size() - 1; i >= 0; i--) {
                    restores.get(i).run();
                }
            }
        }
        if ("prepare".equals(method)) {
            rewriteQuery((StatementHandler) invocation.getTarget());
            return invocation.proceed();
        }
        if ("handleResultSets".equals(method)) {
            Object result = invocation.proceed();
            SensitiveEntities.decryptResult(result, encryptor);
            return result;
        }
        return invocation.proceed();
    }

    private void rewriteQuery(StatementHandler handler) {
        BoundSql boundSql = handler.getBoundSql();
        if (boundSql == null || boundSql.getParameterObject() == null) {
            return;
        }
        MetaObject parameter = SystemMetaObject.forObject(boundSql.getParameterObject());
        String rewritten = SensitiveSqlRewriter.rewrite(boundSql.getSql(), new SensitiveSqlRewriter.ValueAccess() {
            @Override
            public Object get(String path) {
                return parameter.getValue(path);
            }

            @Override
            public void set(String path, Object value) {
                parameter.setValue(path, value);
            }
        }, encryptor);
        if (!rewritten.equals(boundSql.getSql())) {
            SystemMetaObject.forObject(boundSql).setValue("sql", rewritten);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 密钥由 Spring 注入
    }
}
