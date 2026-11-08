import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import Button from "./Button";
import Input from "./Input";
import Select from "./Select";
import FormField from "./FormField";
import PageHeader from "./PageHeader";

describe("componentes fundamentais", () => {
  it("mantém ação, tipo seguro e estado desabilitado do botão", () => {
    const click = vi.fn();
    const { rerender } = render(<Button onClick={click}>Abrir</Button>);
    const botao = screen.getByRole("button", { name: "Abrir" });
    expect(botao).toHaveAttribute("type", "button");
    fireEvent.click(botao);
    expect(click).toHaveBeenCalledOnce();
    rerender(
      <Button disabled onClick={click}>
        Abrir
      </Button>,
    );
    expect(botao).toBeDisabled();
  });
  it("associa rótulo, ajuda e erro ao controle", () => {
    render(
      <FormField id="nome" label="Nome" hint="Nome completo" error="Informe o nome">
        <Input id="nome" aria-invalid="true" aria-describedby="nome-hint nome-error" />
      </FormField>,
    );
    const campo = screen.getByLabelText("Nome");
    expect(campo).toHaveAccessibleDescription("Nome completo Informe o nome");
    expect(campo).toHaveAttribute("aria-invalid", "true");
  });
  it("mantém select nativo, seleção e rótulo", () => {
    const change = vi.fn();
    render(
      <FormField id="ativo" label="Ativo">
        <Select id="ativo" onChange={change}>
          <option value="btc">BTC</option>
          <option value="eth">ETH</option>
        </Select>
      </FormField>,
    );
    fireEvent.change(screen.getByRole("combobox", { name: "Ativo" }), { target: { value: "eth" } });
    expect(change).toHaveBeenCalledOnce();
    expect(screen.getByRole("combobox")).toHaveValue("eth");
  });
  it("apresenta título e descrição sem perder a semântica", () => {
    render(
      <PageHeader
        id="titulo"
        title="Carteiras"
        eyebrow="Cadastros"
        description="Organize suas contas."
      />,
    );
    expect(screen.getByRole("heading", { level: 1, name: "Carteiras" })).toHaveAttribute(
      "id",
      "titulo",
    );
    expect(screen.getByText("Organize suas contas.")).toBeVisible();
  });
});
