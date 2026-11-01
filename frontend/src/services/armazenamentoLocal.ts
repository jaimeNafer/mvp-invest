export const armazenamentoLocal = {
  salvar(chave: string, valor: string): void {
    localStorage.setItem(chave, valor);
  },

  obter(chave: string): string | null {
    return localStorage.getItem(chave);
  },

  remover(chave: string): void {
    localStorage.removeItem(chave);
  },
};
