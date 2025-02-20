var pdfDoc = null;

function createPage() {
    var canvas = document.createElement("canvas");
    document.body.appendChild(canvas);
    return canvas;
}

function renderPage(num) {
    pdfDoc.getPage(num).then(function (page) {
        var viewport = page.getViewport({ scale: 2.0 });
        var canvas = createPage();
        var ctx = canvas.getContext('2d');

        canvas.height = viewport.height;
        canvas.width = viewport.width;

        page.render({
            canvasContext: ctx,
            viewport: viewport
        }).promise.then(() => {});
    });
}

function scrollToPage(pageIndex) {
    const canvasList = document.getElementsByTagName("canvas");
    if (pageIndex < 0 || pageIndex >= canvasList.length) {
        return;
    }
    const targetCanvas = canvasList[pageIndex];
    if (targetCanvas) {
        targetCanvas.scrollIntoView({ behavior: "smooth" });
    }
}

function scrollToWord(x, y) {
    window.scrollTo({
        left: x,
        top: y,
        behavior: "smooth"
    });
}

// Оновлений метод для розшифрування Base64
function receivePDF(base64String) {
    console.log("Отримано Base64 PDF");

    try {
        // Перетворюємо Base64 у бінарні дані
        var binaryString = atob(base64String);
        var bytes = new Uint8Array(binaryString.length);
        for (var i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }

        // Завантажуємо PDF у pdf.js
        pdfjsLib.getDocument({ data: bytes.buffer }).promise.then(function (pdf) {
            pdfDoc = pdf;
            for (var i = 1; i <= pdfDoc.numPages; i++) {
                renderPage(i);
            }
        }).catch(function (error) {
            console.error("Помилка завантаження PDF:", error);
        });

    } catch (e) {
        console.error("Помилка обробки Base64:", e);
    }
}
