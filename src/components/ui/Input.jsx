import React from 'react';
import { cn } from '../../lib/utils';

const Input = React.forwardRef(({
  className,
  type = 'text',
  error,
  label,
  helperText,
  ...props
}, ref) => {
  const inputClasses = cn(
    'flex h-10 w-full rounded-lg border border-surface-border bg-surface-panel px-3 py-2 text-sm text-white placeholder:text-surface-muted focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 focus:ring-offset-surface-bg disabled:cursor-not-allowed disabled:opacity-50',
    error && 'border-danger focus:ring-danger',
    className
  );

  return (
    <div className="space-y-2">
      {label && (
        <label className="text-sm font-medium text-white">
          {label}
        </label>
      )}
      <input
        type={type}
        className={inputClasses}
        ref={ref}
        {...props}
      />
      {error && (
        <p className="text-sm text-danger">{error}</p>
      )}
      {helperText && !error && (
        <p className="text-sm text-surface-muted">{helperText}</p>
      )}
    </div>
  );
});

Input.displayName = 'Input';

export default Input;
