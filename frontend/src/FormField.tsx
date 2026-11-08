import type { ReactNode } from "react";

type Props = {
  id: string;
  label: ReactNode;
  hint?: ReactNode;
  error?: ReactNode;
  children: ReactNode;
};

export default function FormField({ id, label, hint, error, children }: Props) {
  return (
    <div className="ds-field">
      <label htmlFor={id}>{label}</label>
      {children}
      {hint && (
        <p id={`${id}-hint`} className="field-hint">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="message-error">
          {error}
        </p>
      )}
    </div>
  );
}
