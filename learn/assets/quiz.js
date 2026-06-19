function setupQuizCards() {
  document.querySelectorAll('[data-quiz]').forEach((card) => {
    const answer = card.getAttribute('data-answer');
    const feedback = card.querySelector('.quiz-feedback');

    card.querySelectorAll('button[data-option]').forEach((button) => {
      button.addEventListener('click', () => {
        card.querySelectorAll('button[data-option]').forEach((item) => {
          item.classList.remove('correct', 'wrong');
        });

        if (button.getAttribute('data-option') === answer) {
          button.classList.add('correct');
          feedback.textContent = card.getAttribute('data-correct') || '回答正确。';
        }
        else {
          button.classList.add('wrong');
          feedback.textContent = card.getAttribute('data-wrong') || '再想想：先判断这段代码是在组装 Prompt、执行调用，还是读取结果。';
        }
      });
    });
  });
}

document.addEventListener('DOMContentLoaded', setupQuizCards);
