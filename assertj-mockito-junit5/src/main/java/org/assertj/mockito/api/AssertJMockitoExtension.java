package org.assertj.mockito.api;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.mockito.internal.progress.MockingProgressImpl.getDefaultVerificationStrategy;
import static org.mockito.internal.progress.ThreadSafeMockingProgress.mockingProgress;

import org.assertj.core.api.SoftAssertionsProvider;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.assertj.core.util.VisibleForTesting;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

public class AssertJMockitoExtension extends SoftAssertionsExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final ExtensionContext.Namespace SOFT_ASSERTIONS_EXTENSION_NAMESPACE = create(SoftAssertionsExtension.class);

    private final MockitoExtension mockitoExtension = new MockitoExtension();

    @Override
    public void beforeEach(ExtensionContext context) {
        mockitoExtension.beforeEach(context);
        mockingProgress().setVerificationStrategy(verificationMode -> new VerificationWrapper(verificationMode, context));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        mockingProgress().setVerificationStrategy(getDefaultVerificationStrategy());
        mockitoExtension.afterEach(context);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return super.supportsParameter(parameterContext, extensionContext) || mockitoExtension.supportsParameter(parameterContext, extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (super.supportsParameter(parameterContext, extensionContext)) {
            return super.resolveParameter(parameterContext, extensionContext);
        }

        return mockitoExtension.resolveParameter(parameterContext, extensionContext);
    }

    @VisibleForTesting
    static class VerificationWrapper implements VerificationMode {
        private final VerificationMode delegate;
        private final ExtensionContext context;

        @VisibleForTesting
        VerificationWrapper(VerificationMode delegate, ExtensionContext context) {
            this.delegate = delegate;
            this.context = context;
        }

        @Override
        public void verify(VerificationData data) {
            verify(context, data);
        }

        public VerificationMode description(String description) {
            throw new IllegalStateException("Should not fail in this mode");
        }

        private void verify(ExtensionContext context, VerificationData data) {
            SoftAssertionsProvider softly = context.getStore(SOFT_ASSERTIONS_EXTENSION_NAMESPACE).get(SoftAssertionsProvider.class, SoftAssertionsProvider.class);
            if (softly != null) {
                softly.check(() -> this.delegate.verify(data));
            }
        }
    }
}
