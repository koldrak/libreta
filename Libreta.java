
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.StyledEditorKit;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import java.io.InputStream;
import org.jsoup.Jsoup;
import javax.swing.text.DefaultEditorKit;


public class Libreta {
    static DefaultListModel <Nota> Notas = new DefaultListModel <>();
    static ArrayList<Nota> todasLasNotas = new ArrayList<>();
    static JLabel contadorTotal = new JLabel();
    static JLabel contadorHoy = new JLabel();
    // Anchura máxima aproximada de las imágenes en Word (en píxeles)
    private static final int MAX_IMG_WIDTH_PX = 500;
    private static String colorDesdeTexto(String palabra) {
    	 palabra = palabra.toLowerCase();
         int suma = 0;
         for (char c : palabra.toCharArray()) {
             suma += c;
         }
         int r = (suma * 3) % 256;
         int g = (suma * 5) % 256;
         int b = (suma * 7) % 256;
         return String.format("#%02X%02X%02X", r, g, b);
     }

     private static String colorDesdeTitulo(String titulo) {
         String antesSlash = titulo.split("/", 2)[0].trim();
         String[] partes = antesSlash.split("\\s+", 2);
         String primera = partes.length > 0 ? partes[0] : "";
         String resto = partes.length > 1 ? partes[1] : "";

         Color base = Color.decode(colorDesdeTexto(primera));

         if (!resto.isEmpty()) {
             int suma = 0;
             for (char c : resto.toLowerCase().toCharArray()) {
                 if (Character.isDigit(c)) {
                     // pondera los dígitos para que pequeñas diferencias numéricas
                     // produzcan colores más distintos
                     suma += (c - '0') * 25;
                 } else if (c != '.') { // se ignoran puntos decimales
                     suma += c;
                 }
             }

             int deltaR = ((suma * 1) % 201) - 100; // rango [-100,100]
             int deltaG = ((suma * 1) % 201) - 100;
             int deltaB = ((suma * 1) % 201) - 100;

             int r = Math.max(0, Math.min(255, base.getRed() + deltaR));
             int g = Math.max(0, Math.min(255, base.getGreen() + deltaG));
             int b = Math.max(0, Math.min(255, base.getBlue() + deltaB));
             base = new Color(r, g, b);
         }

         return String.format("#%02X%02X%02X", base.getRed(), base.getGreen(), base.getBlue());
    }

    private static Color colorContraste(Color c) {
            // luminancia aproximada
            double y = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255;
            return (y > 0.5) ? Color.black : Color.white;
    }
    private static boolean esHoy(Date fecha) {
        if (fecha == null) return false;
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        return df.format(fecha).equals(df.format(new Date()));
    }

    static void actualizarContadores() {
        todasLasNotas = Collections.list(Notas.elements());
        contadorTotal.setText("Total: " + todasLasNotas.size());
        int hoy = 0;
        for (Nota n : todasLasNotas) {
            if (esHoy(n.fechaCreacion)) hoy++;
        }
        contadorHoy.setText("Hoy: " + hoy);
    }

	public static void main(String[] args) {

		todasLasNotas = Nota.cargarNotas();
		todasLasNotas.sort((a, b) -> a.titulo.compareToIgnoreCase(b.titulo));
		for (Nota n : todasLasNotas) {
			Notas.addElement(n);
		}

		 JFrame mainmenu = new JFrame("Libreta de apuntes");
         mainmenu.setSize(353, 400);
         mainmenu.setAlwaysOnTop(true);
         mainmenu.setLayout(new BorderLayout());
         JPanel Superior = new JPanel();
         JPanel Centro = new JPanel(new BorderLayout());
         JPanel Inferior = new JPanel(new BorderLayout());

         //Funcionamaniento de botones
         JButton boton1 = new JButton("Agregar Nota");
         JButton boton2 = new JButton("Exportar Word");

         actualizarContadores();

		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == boton1) {
					new Formulario ();
				}
			}
		};
		ActionListener listener2 = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == boton2) {
				    exportarNotasAWord();
				}
			}
		};
        //JList con nombres de notas
        JList <Nota> lista = new JList<>(Notas);
        lista.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        if (value instanceof Nota) {
                                String titulo = ((Nota) value).titulo;
                                String hex = colorDesdeTitulo(titulo);

                                if (!isSelected) {
                                        label.setOpaque(true);
                                        Color color = Color.decode(hex);
                                        label.setBackground(color);
                                        label.setForeground(colorContraste(color));
                                }
                        }
                        return label;
                }
        });
        JScrollPane scrollLista = new JScrollPane(lista);
		scrollLista.setPreferredSize(new Dimension(250, 200)); // Opcional: ajustar tamaño visible

		lista.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int index = lista.locationToIndex(e.getPoint()); // Índice del ítem bajo el mouse

				if (index >= 0) {
					lista.setSelectedIndex(index); // Resalta la nota
					Nota seleccionada = lista.getModel().getElementAt(index);

					// Acción para clic derecho
					if (SwingUtilities.isRightMouseButton(e)) {
						int confirmacion = JOptionPane.showConfirmDialog(null,
								"¿Eliminar la nota \"" + seleccionada.titulo + "\"?",
								"Confirmar eliminación", JOptionPane.YES_NO_OPTION);

						if (confirmacion == JOptionPane.YES_OPTION) {
							Libreta.Notas.removeElementAt(index);
                            ArrayList<Nota> respaldo = Collections.list(Notas.elements());
                            Nota.guardarNotas(respaldo);
                            Libreta.actualizarContadores();
						}

						// Acción para clic izquierdo
					} else if (SwingUtilities.isLeftMouseButton(e)) {
						new VentanaNota(seleccionada);
					}
				}
			}
		});


		// Campo de búsqueda
		JTextField buscador = new JTextField(25);
		buscador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));


		// Filtro dinámico
		buscador.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) {
				filtrarNotas();
			}
			public void removeUpdate(javax.swing.event.DocumentEvent e) {
				filtrarNotas();
			}
			public void changedUpdate(javax.swing.event.DocumentEvent e) {
				filtrarNotas();
			}

			private void filtrarNotas() {
				String texto = buscador.getText().toLowerCase();
				Notas.clear();
				for (Nota n : todasLasNotas) {
					String[] palabrasClave = texto.split("\\s+");
					String titulo = n.titulo.toLowerCase();
					boolean coincide = true;

					for (String palabra : palabrasClave) {
						if (!titulo.contains(palabra)) {
							coincide = false;
							break;
						}
					}

					if (coincide) {
						Notas.addElement(n);
					}

				}
			}
		});

		//Orden de ventanas

	      mainmenu.add(Superior, BorderLayout.NORTH);
          boton1.addActionListener(listener);
          boton2.addActionListener(listener2);
          Superior.add(contadorTotal);
          Superior.add(boton1);
          Superior.add(boton2);
          Superior.add(contadorHoy);

		mainmenu.add(Centro, BorderLayout.CENTER);
		Centro.add(scrollLista);     

		mainmenu.add(Inferior, BorderLayout.SOUTH);
		Inferior.add(buscador);

		//cierre de ventana
		mainmenu.setVisible(true);
		mainmenu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
    public static void exportarNotasAWord() {
        try {
            XWPFDocument doc = new XWPFDocument();

            for (Nota nota : todasLasNotas) {
                // Título de la nota
                XWPFParagraph titulo = doc.createParagraph();
                XWPFRun runTitulo = titulo.createRun();
                runTitulo.setText(nota.titulo);
                runTitulo.setBold(true);
                runTitulo.setFontSize(16);

                org.jsoup.nodes.Document htmlDoc = Jsoup.parse(nota.contenidoHTML);
                XWPFParagraph[] actual = new XWPFParagraph[] { doc.createParagraph() };
                for (org.jsoup.nodes.Node nodo : htmlDoc.body().childNodes()) {
                    procesarNodo(nodo, doc, actual, null);
                }

                doc.createParagraph(); // salto entre notas
            }

            try (FileOutputStream out = new FileOutputStream("NotasExportadas.docx")) {
                doc.write(out);
            }

            JOptionPane.showMessageDialog(null, "Notas exportadas correctamente a NotasExportadas.docx");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al exportar: " + e.getMessage());
        }
    }

    /**
     * Reemplaza los saltos de línea ("\n") del documento por etiquetas
     * HTML <br> para que se conserven al guardar la nota.
     */
    static void reemplazarSaltosDeLinea(HTMLDocument doc, HTMLEditorKit kit) {
        try {
            String texto = doc.getText(0, doc.getLength());
            int pos = 0;
            while (pos < texto.length()) {
                char c = texto.charAt(pos);
                if (c == '\r') {
                    boolean hasNewline = pos + 1 < texto.length() && texto.charAt(pos + 1) == '\n';
                    doc.remove(pos, hasNewline ? 2 : 1);
                    kit.insertHTML(doc, pos, "<br>", 0, 0, HTML.Tag.BR);
                    texto = doc.getText(0, doc.getLength());
                    pos += 4;
                } else if (c == '\n') {
                    doc.remove(pos, 1);
                    kit.insertHTML(doc, pos, "<br>", 0, 0, HTML.Tag.BR);
                    texto = doc.getText(0, doc.getLength());
                    pos += 4;
                } else {
                    pos++;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    // Procesa recursivamente el HTML para exportar respetando <br>
    private static void procesarNodo(org.jsoup.nodes.Node nodo, XWPFDocument doc,
                                     XWPFParagraph[] actual, String colorBg) {
        if (nodo instanceof org.jsoup.nodes.TextNode) {
            String texto = ((org.jsoup.nodes.TextNode) nodo).text();
            if (!texto.isEmpty()) {
                XWPFRun r = actual[0].createRun();
                if (colorBg != null) r.setTextHighlightColor(colorBg.replace("#", ""));
                r.setText(texto);
            }
        } else if (nodo instanceof org.jsoup.nodes.Element) {
            org.jsoup.nodes.Element e = (org.jsoup.nodes.Element) nodo;
            String nuevoBg = colorBg;
            String estilo = e.attr("style");
            if (estilo.contains("background-color")) {
                try {
                    nuevoBg = estilo.split("background-color:")[1].split(";")[0].trim();
                } catch (Exception ex) {
                    nuevoBg = colorBg;
                }
            }

            if (e.tagName().equalsIgnoreCase("br")) {
                actual[0] = doc.createParagraph();
                return;
            } else if (e.tagName().equalsIgnoreCase("img")) {
                String src = e.attr("src").trim();
                int ancho = 200;
                int alto = 200;
                try {
                    ancho = Integer.parseInt(e.attr("width"));
                    alto = Integer.parseInt(e.attr("height"));
                } catch (NumberFormatException ex) {
                    // usar valores por defecto
                }
                File imageFile = (src.contains("/") || src.contains("\\")) ? new File(src) : new File("imagenes", src);
                if (imageFile.exists()) {
                    try (InputStream pic = new FileInputStream(imageFile)) {
                        // Si no hay dimensiones, obtenerlas de la imagen
                        if (!e.hasAttr("width") || !e.hasAttr("height")) {
                            try {
                                BufferedImage bimg = ImageIO.read(imageFile);
                                if (bimg != null) {
                                    if (!e.hasAttr("width")) ancho = bimg.getWidth();
                                    if (!e.hasAttr("height")) alto = bimg.getHeight();
                                }
                            } catch (IOException ex) {
                                // ignorar y usar valores por defecto
                            }
                        }

                        // Escala la imagen si excede el máximo permitido
                        if (ancho > MAX_IMG_WIDTH_PX) {
                            double escala = (double) MAX_IMG_WIDTH_PX / ancho;
                            ancho = MAX_IMG_WIDTH_PX;
                            alto = (int) Math.round(alto * escala);
                        }

                        XWPFRun r = actual[0].createRun();
                        r.addPicture(pic, XWPFDocument.PICTURE_TYPE_PNG, src,
                                     Units.toEMU(ancho), Units.toEMU(alto));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    XWPFRun r = actual[0].createRun();
                    r.setItalic(true);
                    r.setText("[Imagen no encontrada: " + src + "]");
                }
            } else {
                for (org.jsoup.nodes.Node child : e.childNodes()) {
                    procesarNodo(child, doc, actual, nuevoBg);
                }
            }
        }
    }
}

class Nota implements Serializable {
	  private static final long serialVersionUID = 5744357576790633364L;
      String titulo;
      String contenidoHTML;
      Date fechaCreacion;

      public Nota (String ti,String con) {
              this(ti, con, new Date());
      }

      public Nota(String ti, String con, Date fecha) {
              titulo = ti;
              contenidoHTML = con;
              fechaCreacion = fecha;
              Libreta.Notas.addElement(this);
      }
	
	public String toString() {
		return titulo;
	}
	
	public static void guardarNotas(ArrayList<Nota> lista) {
        File notasFile = new File("notas.dat");
        File backupDir = new File("respaldos");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        String stamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
        File backupFile = new File(backupDir, "notas_" + stamp + ".dat");

        try {
            if (notasFile.exists()) {
                Files.copy(notasFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(notasFile))) {
            out.writeObject(lista);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Nota> cargarNotas() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("notas.dat"))) {
            return (ArrayList<Nota>) in.readObject();
        } catch (Exception e) {
            File backupDir = new File("respaldos");
            if (backupDir.exists()) {
                File[] backups = backupDir.listFiles((dir, name) -> name.matches("notas_\\d{8}\\.dat"));
                if (backups != null && backups.length > 0) {
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
                    Arrays.sort(backups, (a, b) -> {
                        Matcher ma = Pattern.compile("notas_(\\d{8})\\.dat").matcher(a.getName());
                        Matcher mb = Pattern.compile("notas_(\\d{8})\\.dat").matcher(b.getName());
                        try {
                            if (ma.matches() && mb.matches()) {
                                Date da = df.parse(ma.group(1));
                                Date db = df.parse(mb.group(1));
                                return db.compareTo(da); // descendente
                            }
                        } catch (Exception ex) {
                            // ignorar y tratar como iguales
                        }
                        return 0;
                    });
                    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(backups[0]))) {
                        return (ArrayList<Nota>) in.readObject();
                    } catch (Exception ex) {
                        // ignorar y retornar lista vacía
                    }
                }
            }
            return new ArrayList<>();
        }
    }
}

class Formulario extends JFrame {
	  StringWriter writer = new StringWriter();
      HTMLEditorKit kit = new HTMLEditorKit();
      public String ultimoColorHTML = null;
      ArrayList<ImageIcon> imagenesPegadas = new ArrayList<>();

      private void insertarConColor(HTMLDocument doc, int offset, String texto,
              String color, int size) throws Exception {
          String extra = color.equals("#FFFFFF") ? "; color:#000000" : "";
          String[] partes = texto.split("\\r?\\n", -1);
          int pos = offset;
          int prevLen = doc.getLength();
          for (int i = 0; i < partes.length; i++) {
              String parte = partes[i];
              if (!parte.isEmpty()) {
                  String html = "<span style=\"background-color:" + color
                          + "; font-size:" + size + "pt" + extra + "\">"
                          + parte + "</span>";
                  kit.insertHTML(doc, pos, html, 0, 0, HTML.Tag.SPAN);
                  int newLen = doc.getLength();
                  pos += newLen - prevLen;
                  prevLen = newLen;
              }
              if (i < partes.length - 1) {
                  doc.insertString(pos, "\n", null);
                  pos++;
                  prevLen = doc.getLength();
              }
          }
      }

	private BufferedImage toBufferedImage(Image img) {
	    if (img instanceof BufferedImage) {
	        return (BufferedImage) img;
	    }

	    BufferedImage bimage = new BufferedImage(
	        img.getWidth(null), img.getHeight(null),
	        BufferedImage.TYPE_INT_ARGB
	    );

	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();

	    return bimage;
	}

	private String limpiarTitulo(String titulo) {
	    return titulo.replaceAll("[^a-zA-Z0-9\\-_]", "_");
	}

    public Formulario() {
        this.setTitle("FORMULARIO");
        this.setSize(500, 400);
        this.setAlwaysOnTop(true);
        this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS)); 
        JPanel Inferior = new JPanel(); // usa FlowLayout por defecto
        JPanel Superior = new JPanel(); // usa FlowLayout por defecto
        JPanel Central = new JPanel(new BorderLayout());
        
        //Campo para ingresar titulo de la nota 
        JLabel etiquetatit = new JLabel("Titulo del texto");
        JTextField campotit = new JTextField(30); //crear text field
        campotit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));

        
      //Campo para ingresar texto de la nota 
        JLabel etiquetatxt = new JLabel("ingresa el codigo");
        JTextPane campotxt = new JTextPane() {
        	@Override
        	public void paste() {
        	    Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        	    if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
        	        try {
        	            Image img = (Image) t.getTransferData(DataFlavor.imageFlavor);
        	            ImageIcon icon = new ImageIcon(img);

        	            // Insertamos como icono visual
                        this.insertIcon(icon);
                        // Evita que el icono se replique al escribir
                        StyledEditorKit sek = (StyledEditorKit) this.getEditorKit();
                        sek.getInputAttributes().removeAttribute(StyleConstants.IconAttribute);
                        
        	            // Guardamos temporalmente la imagen en una lista
        	            if (imagenesPegadas == null) {
        	                imagenesPegadas = new ArrayList<>();
        	            }
        	            imagenesPegadas.add(icon);

        	        } catch (Exception ex) {
        	            ex.printStackTrace();
        	        }
        	    } else if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    try {
                        String texto = (String) t.getTransferData(DataFlavor.stringFlavor);
                        texto = texto.replace("\r\n", "\n").replace('\r', '\n');
                        this.replaceSelection(texto);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    super.paste();
                }
            }

        };
        campotxt.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int pos = campotxt.viewToModel2D(e.getPoint()); // <- usa viewToModel2D para precisión
                    StyledDocument doc = campotxt.getStyledDocument();
                    Element elem = doc.getCharacterElement(pos);
                    AttributeSet attr = elem.getAttributes();
                    Icon icon = StyleConstants.getIcon(attr);

                    if (icon instanceof ImageIcon) {
                        ImageIcon imgIcon = (ImageIcon) icon;

                        String anchoStr = JOptionPane.showInputDialog(null, "Ancho (px):", imgIcon.getIconWidth());
                        String altoStr = JOptionPane.showInputDialog(null, "Alto (px):", imgIcon.getIconHeight());

                        if (anchoStr != null && altoStr != null) {
                            try {
                                int ancho = Integer.parseInt(anchoStr);
                                int alto = Integer.parseInt(altoStr);
                                Image nueva = imgIcon.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
                                ImageIcon redimensionado = new ImageIcon(nueva);

                                SimpleAttributeSet sas = new SimpleAttributeSet();
                                StyleConstants.setIcon(sas, redimensionado);

                                // Reemplaza el ícono anterior
                                doc.remove(elem.getStartOffset(), 1);
                                doc.insertString(elem.getStartOffset(), " ", sas);
                            } catch (NumberFormatException | BadLocationException ex) {
                                JOptionPane.showMessageDialog(null, "Entrada inválida.");
                            }
                        }
                    }
                }
            }
        });


        HTMLEditorKit kit = new HTMLEditorKit(); // 🆕
        campotxt.setEditorKit(kit);              // 🆕
        campotxt.setContentType("text/html");    // 🆕
        
        campotxt.setPreferredSize(new Dimension(350, 200));
        JScrollPane scroll = new JScrollPane(campotxt); // Para permitir desplazamiento si el texto es largo
        campotxt.setFont(new Font("Arial", Font.BOLD,16));
        
        // Ctrl + N: aplicar negrita
        campotxt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "negrita" );
        campotxt.getActionMap().put( "negrita" , new StyledEditorKit .BoldAction());
        
        // Enter: iniciar nueva línea sin mantener negrita
        campotxt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "newline-no-bold");
        campotxt.getActionMap().put("newline-no-bold", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StyledEditorKit sek = (StyledEditorKit) campotxt.getEditorKit();
                int size = StyleConstants.getFontSize(sek.getInputAttributes());
                new DefaultEditorKit.InsertBreakAction().actionPerformed(e);
                sek.getInputAttributes().removeAttribute(StyleConstants.Bold);
                sek.getInputAttributes().addAttribute(StyleConstants.FontSize, size);
            }
        });
        
     // Shift + Tab: aplicar color
        campotxt.getInputMap().put(KeyStroke.getKeyStroke("shift TAB"), "aplicarUltimoColor");
        campotxt.getActionMap().put("aplicarUltimoColor", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int start = campotxt.getSelectionStart();
                    int end = campotxt.getSelectionEnd();

                    if (start == end) {
                        // No hay selección → buscar palabra a la izquierda
                        int pos = start - 1;
                        String texto = campotxt.getText(0, campotxt.getDocument().getLength());

                        while (pos >= 0 && Character.isWhitespace(texto.charAt(pos))) pos--;
                        int fin = pos + 1;
                        while (pos >= 0 && Character.isLetterOrDigit(texto.charAt(pos))) pos--;
                        start = pos + 1;
                        end = fin;
                    }

                    if (start >= end || ultimoColorHTML == null) return;

                    StyledEditorKit sek = (StyledEditorKit) campotxt.getEditorKit();
                    int size = StyleConstants.getFontSize(sek.getInputAttributes());

                    String original = campotxt.getDocument().getText(start, end - start);
                    HTMLDocument doc = (HTMLDocument) campotxt.getDocument();
                    doc.remove(start, end - start);
                    insertarConColor(doc, start, original, ultimoColorHTML, size);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Alt + C: cambiar al siguiente color
        campotxt.getInputMap().put(KeyStroke.getKeyStroke("alt C"), "siguienteColor");
        campotxt.getActionMap().put("siguienteColor", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] colores = {"#808080", "#90EE90", "#CC9900", "#87CEEB"};
                int index = -1;
                for (int i = 0; i < colores.length; i++) {
                    if (colores[i].equalsIgnoreCase(ultimoColorHTML)) {
                        index = i;
                        break;
                    }
                }
                int siguiente = (index + 1) % colores.length;
                ultimoColorHTML = colores[siguiente];
                Toolkit.getDefaultToolkit().beep(); // Confirmación
            }
        });

     // Alt + X: cambiar al color "Sin destacar"
        campotxt.getInputMap().put(KeyStroke.getKeyStroke("alt X"), "colorSinDestacar");
        campotxt.getActionMap().put("colorSinDestacar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ultimoColorHTML = "#FFFFFF";
                Toolkit.getDefaultToolkit().beep(); // Confirmación
            }
        });


        
        //boton de guardar nota
        JButton botcrearnota = new JButton("Crear nueva nota");
        
        botcrearnota.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String valortit = campotit.getText(); // obtener valor de text field
               	String valortxt = campotxt.getText(); // obtener valor de text field
               	
                campotxt.setContentType("text/html");
                campotxt.getDocument().putProperty("IgnoreCharsetDirective", Boolean.TRUE);
            	try {
            	    kit.write(writer, campotxt.getDocument(), 0, campotxt.getDocument().getLength());
            	} catch (Exception ex) {
            	    ex.printStackTrace();
            	}
                String html = writer.toString();
                try {
                    StyledDocument doc = campotxt.getStyledDocument();
                    int imgIndex = 0;
                    for (int i = 0; i < doc.getLength(); i++) {
                        Element elem = doc.getCharacterElement(i);
                        AttributeSet attr = elem.getAttributes();
                        Icon icon = StyleConstants.getIcon(attr);

                        if (icon instanceof ImageIcon) {
                            ImageIcon imgIcon = (ImageIcon) icon;
                            String nombreArchivo = limpiarTitulo(valortit) + "_img" + imgIndex + ".png";


                            // Guarda la imagen en disco
                            File imgDir = new File("imagenes");
                            if (!imgDir.exists()) imgDir.mkdir();
                            File output = new File(imgDir, nombreArchivo);
                            ImageIO.write(toBufferedImage(imgIcon.getImage()), "png", output);

                            // Reemplaza el icono embebido por un <img src="imagenes/imgX.png">
                            SimpleAttributeSet sas = new SimpleAttributeSet();
                            StyleConstants.setIcon(sas, null);  // Borra icono visual
                            doc.remove(elem.getStartOffset(), 1);
                            kit.insertHTML((HTMLDocument) doc, elem.getStartOffset(),
                            	    "<img src='" + nombreArchivo + "' width='" + imgIcon.getIconWidth()
                            	    + "' height='" + imgIcon.getIconHeight() + "'>", 0, 0, HTML.Tag.IMG);

                            imgIndex++;
                        }
                    }
                    // Reemplaza los saltos de línea por <br> antes de generar el HTML
                    Libreta.reemplazarSaltosDeLinea((HTMLDocument) doc, kit);

                    // Ahora sí, escribir HTML ya procesado
                    writer = new StringWriter(); // reinicia
                    kit.write(writer, doc, 0, doc.getLength());
                    String htmlFinal = writer.toString();

                    Nota notax = new Nota(valortit, htmlFinal);

                    // Reordenar y guardar
                    ArrayList<Nota> respaldo = Collections.list(Libreta.Notas.elements());
                    respaldo.sort((a, b) -> a.titulo.compareToIgnoreCase(b.titulo));
                    Libreta.Notas.clear();
                    for (Nota n : respaldo) Libreta.Notas.addElement(n);
                    Nota.guardarNotas(respaldo);
                    Libreta.actualizarContadores();
                    Formulario.this.dispose();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                         
            }
        });
        

        
        //Jlist seleccion de color 
        String[] colores = {"Gris", "Verde", "Amarillo", "Celeste","Sin destacar"};
        JList<String> listacolor = new JList<>(colores);

        listacolor.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
            	try {
            	    int start = campotxt.getSelectionStart();
                    int end = campotxt.getSelectionEnd();
                    if (start == end) return;
                    StyledEditorKit sek = (StyledEditorKit) campotxt.getEditorKit();
                    int size = StyleConstants.getFontSize(sek.getInputAttributes());
                    String original = campotxt.getSelectedText();    
                    String seleccionado = listacolor.getSelectedValue();
                    if (seleccionado == null) {
                        return;
                    }

                    switch (seleccionado) {
                    case "Gris":
                        ultimoColorHTML = "#808080";
                        break;
                    case "Verde":
                        ultimoColorHTML = "#90EE90";
                        break;
                    case "Amarillo":
                        ultimoColorHTML = "#CC9900";
                        break;
                    case "Celeste":
                        ultimoColorHTML = "#87CEEB";
                        break;
                    case "Sin destacar":
                        ultimoColorHTML = "#FFFFFF";
                        break;
                    default:
                        break;
                }
                    HTMLDocument doc = (HTMLDocument) campotxt.getDocument();
                    doc.remove(start, end - start);
                    insertarConColor(doc, start, original, ultimoColorHTML, size);
                    listacolor.clearSelection();
                    
            	} catch (Exception ex) {
            	    ex.printStackTrace();
            	}

            	listacolor.setVisible(false);

            }
        });
        
        
        listacolor.setVisible(false);
        
        
        //Boton para cambiar estilo
        JButton destacar = new JButton("Destacar");
        destacar.addActionListener(e -> {
        	if (listacolor.isVisible()==false) {
            	listacolor.setVisible(true);  
        	} else {
        		listacolor.setVisible(false);
        	}
        });
        
        //Spinner
        Integer[] datos = {8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 46, 48, 50, 52, 54, 56, 58, 60, 62, 64, 66, 68, 70, 72, 74, 76, 78, 80, 82, 84, 86, 88, 90, 92, 94, 96, 98, 100 };

        JSpinner lista = new JSpinner(new SpinnerListModel(datos));
        lista.setPreferredSize(new Dimension(100, 30));
        lista.setVisible(true);
        lista.setValue(16);

        lista.addChangeListener(e -> {
            int nuevoTamaño = (int) lista.getValue();
            StyledDocument doc = campotxt.getStyledDocument();
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setFontSize(attrs, nuevoTamaño);
            doc.setCharacterAttributes(0, doc.getLength(), attrs, false);

            StyledEditorKit sek = (StyledEditorKit) campotxt.getEditorKit();
            sek.getInputAttributes().addAttribute(StyleConstants.FontSize, nuevoTamaño);

            campotxt.repaint();
        });

        
        this.add(Superior, BorderLayout.NORTH);
        Superior.add(etiquetatit);
        Superior.add(campotit);
        
        this.add(Central, BorderLayout.CENTER);
        Central.add(etiquetatxt);
        Central.add(scroll);
        
        this.add(Inferior, BorderLayout.SOUTH);
        Inferior.add(destacar);
        Inferior.add(listacolor);
        Inferior.add(lista);
        Inferior.add (botcrearnota);
  

       
        //Cierre
        this.setVisible(true);
    }
}

class VentanaNota extends JFrame {
	public String ultimoColorHTML = null;
    StringWriter writer = new StringWriter();
    HTMLEditorKit kit = new HTMLEditorKit();


    private void insertarConColor(HTMLDocument doc, int offset, String texto,
                                  String color, int size) throws Exception {
        String extra = color.equals("#FFFFFF") ? "; color:#000000" : "";
        String[] partes = texto.split("\\r?\\n", -1);
        int pos = offset;
        int prevLen = doc.getLength();
        for (int i = 0; i < partes.length; i++) {
            String parte = partes[i];
            if (!parte.isEmpty()) {
            	String html = "<span style=\"background-color:" + color
                        + "; font-size:" + size + "pt" + extra + "\">"
                        + parte + "</span>";
                kit.insertHTML(doc, pos, html, 0, 0, HTML.Tag.SPAN);
                int newLen = doc.getLength();
                pos += newLen - prevLen;
                prevLen = newLen;
            }
            if (i < partes.length - 1) {
                doc.insertString(pos, "\n", null);
                pos++;
                prevLen = doc.getLength();
            }
        }
    }
	
	private BufferedImage toBufferedImage(Image img) {
	    if (img instanceof BufferedImage) {
	        return (BufferedImage) img;
	    }

	    BufferedImage bimage = new BufferedImage(
	        img.getWidth(null), img.getHeight(null),
	        BufferedImage.TYPE_INT_ARGB
	    );

	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();

	    return bimage;
	}
	private String limpiarTitulo(String titulo) {
	    return titulo.replaceAll("[^a-zA-Z0-9\\-_]", "_");
	}

	public VentanaNota(Nota not) {
        this.setTitle(not.titulo);
        this.setAlwaysOnTop(true);
        this.setSize(400, 400);
        this.setLayout(new BorderLayout());
        // Campo para editar el titulo
        JPanel panelSuperior = new JPanel();
        JLabel labelTitulo = new JLabel("Titulo:");
        JTextField campoTitulo = new JTextField(not.titulo, 20);
        panelSuperior.add(labelTitulo);
        panelSuperior.add(campoTitulo);
        this.add(panelSuperior, BorderLayout.NORTH);
        
        JPanel panelInferior = new JPanel(); // usa FlowLayout por defecto
        this.add(panelInferior, BorderLayout.SOUTH);
        
        //Area de texto
        JTextPane areatexto = new JTextPane() {
            @Override
            public void paste() {
                Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    try {
                        Image img = (Image) t.getTransferData(DataFlavor.imageFlavor);
                        ImageIcon icon = new ImageIcon(img);
                        this.insertIcon(icon);
                        
                        // Evita que el icono se replique al escribir
                        StyledEditorKit sek = (StyledEditorKit) this.getEditorKit();
                        sek.getInputAttributes().removeAttribute(StyleConstants.IconAttribute);
                        
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    try {
                        String texto = (String) t.getTransferData(DataFlavor.stringFlavor);
                        texto = texto.replace("\r\n", "\n").replace('\r', '\n');
                        this.replaceSelection(texto);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    super.paste();
                }
            }
        };
        areatexto.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int pos = areatexto.viewToModel2D(e.getPoint()); // <- usa viewToModel2D para precisión
                    StyledDocument doc = areatexto.getStyledDocument();
                    Element elem = doc.getCharacterElement(pos);
                    AttributeSet attr = elem.getAttributes();
                    Icon icon = StyleConstants.getIcon(attr);

                    if (icon instanceof ImageIcon) {
                        ImageIcon imgIcon = (ImageIcon) icon;

                        String anchoStr = JOptionPane.showInputDialog(null, "Ancho (px):", imgIcon.getIconWidth());
                        String altoStr = JOptionPane.showInputDialog(null, "Alto (px):", imgIcon.getIconHeight());

                        if (anchoStr != null && altoStr != null) {
                            try {
                                int ancho = Integer.parseInt(anchoStr);
                                int alto = Integer.parseInt(altoStr);
                                Image nueva = imgIcon.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
                                ImageIcon redimensionado = new ImageIcon(nueva);

                                SimpleAttributeSet sas = new SimpleAttributeSet();
                                StyleConstants.setIcon(sas, redimensionado);

                                // Reemplaza el ícono anterior
                                doc.remove(elem.getStartOffset(), 1);
                                doc.insertString(elem.getStartOffset(), " ", sas);
                            } catch (NumberFormatException | BadLocationException ex) {
                                JOptionPane.showMessageDialog(null, "Entrada inválida.");
                            }
                        }
                    }
                }
            }
        });


        
        areatexto.setEditorKit(kit);
        areatexto.setContentType("text/html");

        HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
        try {
            // Establece la base URL para que las etiquetas <img src="imagenes/archivo.png"> funcionen
            java.net.URL baseURL = new java.io.File("imagenes").toURI().toURL();
            doc.setBase(baseURL);

            kit.read(new java.io.StringReader(not.contenidoHTML), doc, 0);
            areatexto.setDocument(doc);
            
            try {
                HTMLDocument htmlDoc = (HTMLDocument) areatexto.getDocument();
                ElementIterator iterator = new ElementIterator(htmlDoc);
                Element elem;

                while ((elem = iterator.next()) != null) {
                    AttributeSet attrs = elem.getAttributes();
                    Object name = attrs.getAttribute(StyleConstants.NameAttribute);

                    if (name == HTML.Tag.IMG) {
                        String src = (String) attrs.getAttribute(HTML.Attribute.SRC);
                        String widthStr = (String) attrs.getAttribute(HTML.Attribute.WIDTH);
                        String heightStr = (String) attrs.getAttribute(HTML.Attribute.HEIGHT);

                        int ancho = (widthStr != null) ? Integer.parseInt(widthStr) : 200;
                        int alto = (heightStr != null) ? Integer.parseInt(heightStr) : 200;

                        File imgFile = new File("imagenes", src);
                        if (!imgFile.exists()) continue;

                        ImageIcon icon = new ImageIcon(new ImageIcon(imgFile.getAbsolutePath())
                                .getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH));

                        SimpleAttributeSet sas = new SimpleAttributeSet();
                        StyleConstants.setIcon(sas, icon);

                        htmlDoc.remove(elem.getStartOffset(), elem.getEndOffset() - elem.getStartOffset());
                        htmlDoc.insertString(elem.getStartOffset(), " ", sas);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }


        areatexto.setEditable(true);
        JScrollPane scrollarea = new JScrollPane(areatexto); // Para permitir desplazamiento si el texto es largo
        areatexto.setFont(new Font("Arial", Font.BOLD,16));
     // Ctrl + N: aplicar negrita
        areatexto.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "negrita" );
        areatexto.getActionMap().put( "negrita" , new StyledEditorKit .BoldAction());
        // Enter: iniciar nueva línea sin mantener negrita
        areatexto.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "newline-no-bold");
        areatexto.getActionMap().put("newline-no-bold", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {         
                StyledEditorKit sek = (StyledEditorKit) areatexto.getEditorKit();
                int size = StyleConstants.getFontSize(sek.getInputAttributes());
                new DefaultEditorKit.InsertBreakAction().actionPerformed(e);
                sek.getInputAttributes().removeAttribute(StyleConstants.Bold);
                sek.getInputAttributes().addAttribute(StyleConstants.FontSize, size);
            }
        });
        
        areatexto.setEditable(true);
        this.add(scrollarea);
        
        
     // Shift + Tab: aplicar color
        areatexto.getInputMap().put(KeyStroke.getKeyStroke("shift TAB"), "aplicarUltimoColor");
        areatexto.getActionMap().put("aplicarUltimoColor", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int start = areatexto.getSelectionStart();
                    int end = areatexto.getSelectionEnd();

                    if (start == end) {
                        // No hay selección → buscar palabra a la izquierda
                        int pos = start - 1;
                        String texto = areatexto.getText(0, areatexto.getDocument().getLength());

                        while (pos >= 0 && Character.isWhitespace(texto.charAt(pos))) pos--;
                        int fin = pos + 1;
                        while (pos >= 0 && Character.isLetterOrDigit(texto.charAt(pos))) pos--;
                        start = pos + 1;
                        end = fin;
                    }

                    if (start >= end || ultimoColorHTML == null) return;

                    StyledEditorKit sek = (StyledEditorKit) areatexto.getEditorKit();
                    int size = StyleConstants.getFontSize(sek.getInputAttributes());

                    String original = areatexto.getDocument().getText(start, end - start);
                    HTMLDocument doc = (HTMLDocument) areatexto.getDocument();
                    doc.remove(start, end - start);
                    insertarConColor(doc, start, original, ultimoColorHTML, size);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Alt + C: cambiar al siguiente color
        areatexto.getInputMap().put(KeyStroke.getKeyStroke("alt C"), "siguienteColor");
        areatexto.getActionMap().put("siguienteColor", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] colores = {"#808080", "#90EE90", "#CC9900", "#87CEEB"};
                int index = -1;
                for (int i = 0; i < colores.length; i++) {
                    if (colores[i].equalsIgnoreCase(ultimoColorHTML)) {
                        index = i;
                        break;
                    }
                }
                int siguiente = (index + 1) % colores.length;
                ultimoColorHTML = colores[siguiente];
                Toolkit.getDefaultToolkit().beep(); // Confirmación
            }
        });

        // Alt + X: cambiar al color "Sin destacar"
        areatexto.getInputMap().put(KeyStroke.getKeyStroke("alt X"), "colorSinDestacar");
        areatexto.getActionMap().put("colorSinDestacar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ultimoColorHTML = "#FFFFFF";
                Toolkit.getDefaultToolkit().beep(); // Confirmación
            }
        });


       
        //Boton de guardado
        JButton guardar = new JButton("Guardar cambios");
        guardar.addActionListener(e -> {
            int imgIndex = 0;
            // Actualiza el titulo de la nota con el valor del campo
            not.titulo = campoTitulo.getText();
            VentanaNota.this.setTitle(not.titulo);

            try {
                // Recorre el documento para detectar imágenes
                for (int i = 0; i < doc.getLength(); i++) {
                    Element elem = doc.getCharacterElement(i);
                    AttributeSet attr = elem.getAttributes();
                    Icon icon = StyleConstants.getIcon(attr);

                    if (icon instanceof ImageIcon) {
                        ImageIcon imgIcon = (ImageIcon) icon;
                        String nombreArchivo = limpiarTitulo(not.titulo) + "_img" + imgIndex + ".png";

                        // Guardar imagen en disco
                        File imgDir = new File("imagenes");
                        if (!imgDir.exists()) imgDir.mkdir();
                        File output = new File(imgDir, nombreArchivo);
                        ImageIO.write(toBufferedImage(imgIcon.getImage()), "png", output);

                        // Reemplazar ícono por <img src=...>
                        SimpleAttributeSet sas = new SimpleAttributeSet();
                        StyleConstants.setIcon(sas, null);  // Borra ícono visual
                        doc.remove(elem.getStartOffset(), 1);

                        kit.insertHTML((HTMLDocument) doc, elem.getStartOffset(),
                            "<img src='" + nombreArchivo + "' width='" + imgIcon.getIconWidth()
                            + "' height='" + imgIcon.getIconHeight() + "'>", 0, 0, HTML.Tag.IMG);

                        imgIndex++;
                    }
                }
                // Reemplaza los saltos de línea por <br> antes de generar el HTML
                Libreta.reemplazarSaltosDeLinea(doc, kit);
                
                // Generar HTML final ya con <img src='...'>
                writer = new StringWriter(); // reinicia
                kit.write(writer, doc, 0, doc.getLength());
                not.contenidoHTML = writer.toString();

                // Guardar notas
                ArrayList<Nota> respaldo = Collections.list(Libreta.Notas.elements());
                respaldo.sort((a, b) -> a.titulo.compareToIgnoreCase(b.titulo));
                Libreta.Notas.clear();
                for (Nota n : respaldo) {
                    Libreta.Notas.addElement(n);
                }
                Nota.guardarNotas(respaldo);

                VentanaNota.this.dispose();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


      //Spinner
        Integer[] datos = {8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 46, 48, 50, 52, 54, 56, 58, 60, 62, 64, 66, 68, 70, 72, 74, 76, 78, 80, 82, 84, 86, 88, 90, 92, 94, 96, 98, 100 };

        JSpinner lista = new JSpinner(new SpinnerListModel(datos));
        lista.setPreferredSize(new Dimension(100, 30));
        lista.setVisible(true);
        lista.setValue(16);

        lista.addChangeListener(e -> {
            int nuevoTamaño = (int) lista.getValue();
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setFontSize(attrs, nuevoTamaño);
            doc.setCharacterAttributes(0, doc.getLength(), attrs, false);

            StyledEditorKit sek = (StyledEditorKit) areatexto.getEditorKit();
            sek.getInputAttributes().addAttribute(StyleConstants.FontSize, nuevoTamaño);

            areatexto.repaint();
        });
   

        //Jlist seleccion de color 
        String[] colores = {"Gris", "Verde", "Amarillo", "Celeste","Sin destacar"};
        JList<String> listacolor = new JList<>(colores);

        listacolor.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
            	try {
            		int start = areatexto.getSelectionStart();
                    int end = areatexto.getSelectionEnd();
                    if (start == end) return;
                    StyledEditorKit sek = (StyledEditorKit) areatexto.getEditorKit();
                    int size = StyleConstants.getFontSize(sek.getInputAttributes());

                    String seleccionado = listacolor.getSelectedValue();
                    String original = areatexto.getSelectedText();
                    String nuevoTexto;

                    switch (seleccionado) {
                    case "Gris":
                        ultimoColorHTML = "#808080";
                        break;
                    case "Verde":
                        ultimoColorHTML = "#90EE90";
                        break;
                    case "Amarillo":
                        ultimoColorHTML = "#CC9900";
                        break;
                    case "Celeste":
                        ultimoColorHTML = "#87CEEB";
                        break;
                    case "Sin destacar":
                        ultimoColorHTML = "#FFFFFF";
                        break;
                    default:
                        break;
                }

                    doc.remove(start, end - start);
                    insertarConColor(doc, start, original, ultimoColorHTML, size);
                    listacolor.clearSelection();
            	} catch (Exception ex) {
            	    ex.printStackTrace();
            	}

            	listacolor.setVisible(false);



            }
        });
        
        
        listacolor.setVisible(false);
        
        
        //Boton para cambiar estilo
        JButton destacar = new JButton("Destacar");
        destacar.addActionListener(e -> {
        	if (listacolor.isVisible()==false) {
            	listacolor.setVisible(true);  
        	} else {
        		listacolor.setVisible(false);
        	}
        });       
        
        this.add(panelInferior, BorderLayout.SOUTH);
        panelInferior.add(destacar);
        panelInferior.add(listacolor);
        panelInferior.add(guardar);
        panelInferior.add(lista);

        
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setVisible(true);
    }
	
}
