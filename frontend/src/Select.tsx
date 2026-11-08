import type { ComponentProps } from "react";

export default function Select({ className = "", ...props }: ComponentProps<"select">) {
  return <select {...props} className={`ds-control ${className}`.trim()} />;
}
