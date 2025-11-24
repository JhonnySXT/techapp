const STORAGE_KEY = "calorie-buddy-state";

const defaultState = {
  goal: 2000,
  entries: [],
};

class CalorieWorker {
  constructor() {
    this.state = this.loadState();
    this.cacheDom();
    this.bindEvents();
    this.render();
  }

  cacheDom() {
    this.goalInput = document.getElementById("goalInput");
    this.goalSaveBtn = document.getElementById("goalSaveBtn");
    this.summaryCalories = document.getElementById("summaryCalories");
    this.summaryRemaining = document.getElementById("summaryRemaining");
    this.progressBar = document.getElementById("progressBar");
    this.entryForm = document.getElementById("entryForm");
    this.entryList = document.getElementById("entryList");
    this.clearBtn = document.getElementById("clearBtn");
    this.entryTemplate = document.getElementById("entryTemplate");
  }

  bindEvents() {
    this.goalInput.value = this.state.goal;

    this.goalSaveBtn.addEventListener("click", () => {
      const value = Number(this.goalInput.value);
      if (value < 500) {
        alert("Цель должна быть не меньше 500 ккал");
        return;
      }
      this.state.goal = value;
      this.persist();
      this.renderSummary();
    });

    this.entryForm.addEventListener("submit", (event) => {
      event.preventDefault();
      const formData = new FormData(this.entryForm);
      const title = formData.get("title").trim();
      const calories = Number(formData.get("calories"));
      const category = formData.get("category");

      if (!title || Number.isNaN(calories)) {
        alert("Введите корректные данные");
        return;
      }

      this.state.entries.unshift({
        id: crypto.randomUUID(),
        title,
        calories,
        category,
        createdAt: new Date().toISOString(),
      });

      this.entryForm.reset();
      this.entryForm.elements.title.focus();
      this.persist();
      this.renderList();
      this.renderSummary();
    });

    this.clearBtn.addEventListener("click", () => {
      if (this.state.entries.length === 0) return;
      const confirmed = confirm("Очистить все записи?");
      if (!confirmed) return;
      this.state.entries = [];
      this.persist();
      this.render();
    });
  }

  loadState() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : structuredClone(defaultState);
    } catch (error) {
      console.warn("Не удалось загрузить состояние:", error);
      return structuredClone(defaultState);
    }
  }

  persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this.state));
  }

  render() {
    this.renderList();
    this.renderSummary();
  }

  renderList() {
    this.entryList.innerHTML = "";
    if (this.state.entries.length === 0) {
      const empty = document.createElement("p");
      empty.textContent = "Записей пока нет — начинаем вести дневник!";
      empty.className = "empty";
      this.entryList.appendChild(empty);
      return;
    }

    this.state.entries.forEach((entry) => {
      const node = this.entryTemplate.content.cloneNode(true);
      node.querySelector(".entry__title").textContent = entry.title;
      node.querySelector(".entry__category").textContent = this.translateCategory(
        entry.category
      );
      node.querySelector(".entry__calories").textContent = `${entry.calories} ккал`;
      node.querySelector("button").addEventListener("click", () => {
        this.removeEntry(entry.id);
      });
      this.entryList.appendChild(node);
    });
  }

  renderSummary() {
    const total = this.state.entries.reduce((sum, entry) => sum + entry.calories, 0);
    const remaining = Math.max(this.state.goal - total, 0);
    const percent = Math.min((total / this.state.goal) * 100, 100);

    this.summaryCalories.textContent = total;
    this.summaryRemaining.textContent = remaining;
    this.progressBar.style.width = `${percent}%`;

    this.summaryRemaining.closest("p").classList.toggle("text-danger", remaining === 0);
  }

  removeEntry(id) {
    this.state.entries = this.state.entries.filter((entry) => entry.id !== id);
    this.persist();
    this.render();
  }

  translateCategory(category) {
    switch (category) {
      case "breakfast":
        return "Завтрак";
      case "lunch":
        return "Обед";
      case "snack":
        return "Перекус";
      case "dinner":
        return "Ужин";
      default:
        return "Другое";
    }
  }
}

document.addEventListener("DOMContentLoaded", () => new CalorieWorker());






