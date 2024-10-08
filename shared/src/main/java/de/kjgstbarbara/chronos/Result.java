package de.kjgstbarbara.chronos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface Result {
    default boolean isSuccess() {
        return this instanceof Success;
    }

    default boolean isError() {
        return !this.isSuccess();
    }

    String getErrorMessage();

    default Result and(Result result) {
        if(result == null) {
            return this;
        }
        if(this.isSuccess() && result.isSuccess()) {
            return this;
        }
        if(this.isError() && result.isSuccess()) {
            return this;
        }
        if(this.isSuccess() && result.isError()) {
            return result;
        }
        if(this instanceof MultiError multiError) {
            multiError.addError(result);
        }
        if(result instanceof MultiError multiError) {
            multiError.addError(this);
        }
        return new MultiError(this, result);
    }

    static Result error(String errorMessage) {
        return new Error(errorMessage);
    }

    static Result success() {
        return new Success();
    }

    class Success implements Result {
        public Success() {
        }

        @Override
        public String getErrorMessage() {
            throw new IllegalCallerException("The Operation was successful, there is no Error Message. You should check result before calling");
        }
    }

    record Error(String errorMessage) implements Result {
        @Override
        public String getErrorMessage() {
            return errorMessage;
        }
    }

    class MultiError implements Result{
        private final List<String> errorMessages = new ArrayList<>();

        public MultiError(String... errorMessages) {
            this.errorMessages.addAll(List.of(errorMessages));
        }

        public MultiError(Result... errors) {
            this.errorMessages.addAll(Arrays.stream(errors).map(Result::getErrorMessage).toList());
        }

        public void addError(String errorMessage) {
            this.errorMessages.add(errorMessage);
        }

        public void addError(Result error) {
            if(error instanceof MultiError multiError) {
                this.errorMessages.addAll(multiError.errorMessages);
                return;
            }
            this.errorMessages.add(error.getErrorMessage());
        }

        @Override
        public String getErrorMessage() {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append(errorMessages.size()).append(" error(s) occurred:");
            for(int i = 0; i < errorMessages.size(); i++) {
                errorMessage.append((i + 1)).append(". ").append(errorMessage);
            }
            return errorMessage.toString();
        }
    }
}
